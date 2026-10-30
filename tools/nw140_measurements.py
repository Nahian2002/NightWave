"""Private NW140 diagnostic receipts. No device control, timing gate or rating authority."""
from __future__ import annotations
import argparse
import copy
import hashlib
import importlib.util
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROTOCOL = "NW140_DIAGNOSTIC_V1"
METRICS = {
    "art_query_runner_wall_including_debounce": "REAL_MONOTONIC_RUN_BLOCKING",
    "compose_fixture_dispatch_to_verified_visible_row": "COMPOSE_AUTO_ADVANCE_DIAGNOSTIC",
}
ENV = {"model", "sdk", "abis", "vm", "width_px", "height_px", "density_dpi", "font_scale",
       "theme", "reduce_motion", "compilation"}
FIELDS = {"schema", "kind", "metric", "clock_policy", "runtime", "source_sha256", "input_count",
          "variant", "apk_sha256", "test_apk_sha256", "fixture_id", "fixture_count", "fixture_sha256",
          "external_actions", "warmups_per_case", "samples_per_case", "samples", "frames",
          "frame_callbacks_dropped", "frames_truncated", "frames_scope", "environment"}

def sha(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""): h.update(block)
    return h.hexdigest().upper()

def fixture_digest():
    h = hashlib.sha256()
    for i in range(10_000):
        title = f"Fixture probe{i:05}" if i % 2 == 0 else f"পরীক্ষা নমুনা{i:05}"
        h.update(f"{i}|{title}|https://www.youtube.com/watch?v={i:011}\n".encode("utf-8"))
    return h.hexdigest().upper()

def require(condition, field):
    if not condition: raise ValueError("Invalid or unmatched " + field)

def integer(value, low, high): return type(value) is int and low <= value <= high

def percentile(values, fraction):
    require(bool(values) and all(type(v) in (int, float) and math.isfinite(v) and v >= 0 for v in values), "samples")
    ordered = sorted(values)
    return ordered[max(0, math.ceil(len(ordered) * fraction) - 1)]

def analyze(receipt, identity):
    require(set(receipt) == FIELDS, "receipt fields")
    require(integer(receipt["schema"], 1, 1) and receipt["kind"] == "NW140_DIAGNOSTIC_ONLY", "diagnostic kind")
    require(identity["protocol"] == PROTOCOL, "protocol")
    require(type(identity["harness_sha256"]) is str and len(identity["harness_sha256"]) == 64 and
            all(c in "0123456789ABCDEF" for c in identity["harness_sha256"]), "harness identity")
    require(receipt["metric"] in METRICS and receipt["clock_policy"] == METRICS[receipt["metric"]], "metric/clock")
    require(receipt["runtime"] == "ANDROID_ART" and receipt["variant"] == "debug", "runtime/variant")
    for key in ("source_sha256", "apk_sha256", "test_apk_sha256", "input_count"):
        require(receipt[key] == identity[key], key)
    for key in ("source_sha256", "apk_sha256", "test_apk_sha256"):
        require(type(receipt[key]) is str and len(receipt[key]) == 64 and
                all(c in "0123456789ABCDEF" for c in receipt[key]), key)
    require(integer(receipt["input_count"], 50, 100_000), "input count")
    require(receipt["fixture_id"] == "NW140_SYNTHETIC_10K_V1" and receipt["fixture_count"] == 10_000 and
            receipt["fixture_sha256"] == fixture_digest(), "fixture")
    require(receipt["external_actions"] == 0 and type(receipt["external_actions"]) is int, "external actions")
    require(receipt["warmups_per_case"] == 4 and receipt["samples_per_case"] == 20, "sample protocol")
    samples = receipt["samples"]
    require(type(samples) is list and len(samples) == 40, "sample count")
    groups = {name: [] for name in ("latin_tail", "bangla_tail")}
    seen = set()
    for item in samples:
        require(set(item) == {"case_id", "index", "elapsed_ns", "correct"}, "sample fields")
        case, index = item["case_id"], item["index"]
        require(case in groups and integer(index, 0, 19) and (case, index) not in seen, "sample identity")
        require(integer(item["elapsed_ns"], 1, 120_000_000_000) and item["correct"] is True, "correct/timed result")
        seen.add((case, index)); groups[case].append(item["elapsed_ns"] / 1_000_000)
    require(all(len(v) == 20 for v in groups.values()), "per-case sample count")
    env = receipt["environment"]
    require(set(env) == ENV, "environment fields")
    for key in ("model", "abis", "vm"):
        require(type(env[key]) is str and 0 < len(env[key]) <= 120 and "@" not in env[key], key)
    for key in ("width_px", "height_px", "density_dpi"):
        require(integer(env[key], 1, 20_000), key)
    require(integer(env["sdk"], 26, 99), "sdk")
    require(type(env["font_scale"]) in (float, int) and math.isfinite(env["font_scale"]) and
            .5 <= env["font_scale"] <= 4, "font scale")
    require(env["theme"] == "MIDNIGHT" and env["reduce_motion"] is True and
            env["compilation"] == "UNCONTROLLED_DEBUG", "render/compilation policy")
    frames = receipt["frames"]
    require(type(frames) is list and len(frames) <= 10_000, "frame bound")
    require(integer(receipt["frame_callbacks_dropped"], 0, 10**15) and
            type(receipt["frames_truncated"]) is bool, "frame completeness")
    art_only = receipt["metric"].startswith("art_")
    require(receipt["frames_scope"] == ("NONE" if art_only else "WHOLE_FIXTURE_INCLUDING_SETUP_WARMUPS"), "frame scope")
    require(not art_only or (not frames and receipt["frame_callbacks_dropped"] == 0 and not receipt["frames_truncated"]), "ART frame absence")
    for frame in frames:
        require(set(frame) == {"total_ns", "first_draw", "deadline_ns"}, "frame fields")
        require(integer(frame["total_ns"], -1, 120_000_000_000) and integer(frame["first_draw"], -1, 1) and
                integer(frame["deadline_ns"], -1, 120_000_000_000), "frame values")
    usable = [f for f in frames if f["first_draw"] == 0 and f["total_ns"] > 0 and f["deadline_ns"] > 0]
    complete = bool(frames) and len(usable) + sum(f["first_draw"] == 1 for f in frames) == len(frames)
    score_frames = complete and len(usable) >= 100 and receipt["frame_callbacks_dropped"] == 0 and not receipt["frames_truncated"]
    return {
        "status": "STRUCTURALLY_VALID_DIAGNOSTIC_ONLY", "protocol": PROTOCOL,
        "metric": receipt["metric"], "clock_policy": receipt["clock_policy"], "environment": env,
        "source_sha256": receipt["source_sha256"], "apk_sha256": receipt["apk_sha256"],
        "harness_sha256": identity["harness_sha256"],
        "test_apk_sha256": receipt["test_apk_sha256"],
        "cases": {name: {"count": len(values), "p50_ms": percentile(values, .5), "p95_ms": percentile(values, .95),
                          "max_ms": max(values), "raw_ms": values} for name, values in groups.items()},
        "frames": {"scope": receipt["frames_scope"], "observed": len(frames), "first_draw_excluded": sum(f["first_draw"] == 1 for f in frames),
                   "eligible": len(usable), "callbacks_dropped": receipt["frame_callbacks_dropped"], "truncated": receipt["frames_truncated"],
                   "diagnostic_deadline_miss_percent": 100 * sum(f["total_ns"] >= f["deadline_ns"] for f in usable) / len(usable) if score_frames else None},
        "acceptance": {"startup_ttid_ttfd": "NOT_RUN", "real_input_to_display": "NOT_RUN", "audible_next": "NOT_RUN",
                       "production_jank": "NOT_RUN", "allocation_memory_battery_soak": "NOT_RUN", "rating": "NOT_ASSESSED"},
        "limit": "Structural checks do not authenticate execution. Debug/instrumentation/Compose clock and scroll overhead are not controlled app-wide performance. No pass or speedup claim."
    }

def compare(before, after):
    for key in ("protocol", "metric", "clock_policy", "environment", "harness_sha256"):
        require(before[key] == after[key], "comparison " + key)
    require(set(before["cases"]) == set(after["cases"]), "comparison cases")
    return {"status": "DIAGNOSTIC_COMPARISON_ONLY", "before_apk": before["apk_sha256"], "after_apk": after["apk_sha256"],
            "cases": {name: {"p95_delta_ms": after["cases"][name]["p95_ms"] - before["cases"][name]["p95_ms"]}
                      for name in before["cases"]}, "acceptance": "NOT_CERTIFIED", "rating": "NOT_ASSESSED"}

def read_json(path):
    require(Path(path).stat().st_size <= 2 * 1024 * 1024, "receipt size")
    def unique(pairs):
        result = {}
        for key, value in pairs:
            require(key not in result, "duplicate JSON key")
            result[key] = value
        return result
    return json.loads(Path(path).read_text(encoding="utf-8-sig"), object_pairs_hook=unique,
                      parse_constant=lambda _: (_ for _ in ()).throw(ValueError("Nonfinite JSON")))

def private_path(path):
    target = Path(path).resolve()
    require(not target.is_relative_to(ROOT.resolve()), "private output outside repository")
    require(not target.exists(), "new output path")
    return target

def prepare(provenance, apk, test_apk, output):
    p = read_json(provenance)
    spec = importlib.util.spec_from_file_location("nw126", ROOT / "tools/nw126_acceptance.py")
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    digest, count = module.source_hash(ROOT)
    require(p["status"] == "PASS_EMBEDDED_SOURCE_MATCH" and p["sourceSha256"] == digest and
            p["inputCount"] == count and p["apkSha256"] == sha(apk), "current built APK/source")
    require(Path(test_apk).is_file(), "compiled test APK")
    identity = {"protocol": PROTOCOL, "source_sha256": digest, "input_count": count, "apk_sha256": sha(apk),
                "test_apk_sha256": sha(test_apk), "harness_sha256": sha(ROOT / "app/src/androidTest/java/com/nightwave/player/v2/ui/NW140PerformanceDiagnostic.kt"),
                "fixture_sha256": fixture_digest(), "device_execution": "NOT_RUN", "rating": "NOT_ASSESSED"}
    out = private_path(output); out.mkdir(parents=True)
    (out / "identity.json").write_text(json.dumps(identity, indent=2) + "\n", encoding="utf-8")
    (out / "acceptance.json").write_text(json.dumps({"status": "PREPARED_NOT_EXECUTED", "all_device_cases": "NOT_RUN"}, indent=2) + "\n", encoding="utf-8")
    return identity

def self_test():
    identity = dict(protocol=PROTOCOL, source_sha256="A" * 64, apk_sha256="B" * 64, test_apk_sha256="C" * 64, harness_sha256="E" * 64, input_count=943)
    r = dict(schema=1, kind="NW140_DIAGNOSTIC_ONLY", metric="art_query_runner_wall_including_debounce",
             clock_policy="REAL_MONOTONIC_RUN_BLOCKING", runtime="ANDROID_ART", variant="debug",
             fixture_id="NW140_SYNTHETIC_10K_V1", fixture_count=10_000, fixture_sha256=fixture_digest(), external_actions=0,
             warmups_per_case=4, samples_per_case=20, samples=[dict(case_id=case, index=i, elapsed_ns=(i + 1)*1_000_000, correct=True)
             for case in ("latin_tail", "bangla_tail") for i in range(20)], frames=[], frame_callbacks_dropped=0,
             frames_truncated=False, frames_scope="NONE", environment=dict(model="Synthetic", sdk=36, abis="arm64-v8a", vm="Dalvik",
             width_px=1080, height_px=2340, density_dpi=420, font_scale=1., theme="MIDNIGHT", reduce_motion=True, compilation="UNCONTROLLED_DEBUG"))
    r.update({k:identity[k] for k in ("source_sha256", "apk_sha256", "test_apk_sha256", "input_count")})
    result = analyze(r, identity); assert result["cases"]["latin_tail"]["p50_ms"] == 10 and result["cases"]["latin_tail"]["p95_ms"] == 19
    rejected = 0
    mutations = [lambda x:x.update(runtime="HOST_JVM"), lambda x:x.update(source_sha256="D"*64),
                 lambda x:x.update(apk_sha256="D"*64), lambda x:x.update(test_apk_sha256="D"*64),
                 lambda x:x.update(external_actions=1), lambda x:x.update(samples=x["samples"][:-1]),
                 lambda x:x["samples"][0].update(correct=False), lambda x:x["samples"][0].update(elapsed_ns=-1),
                 lambda x:x["samples"][0].update(elapsed_ns=True), lambda x:x["samples"][0].update(elapsed_ns=float("nan")),
                 lambda x:x["samples"][0].update(index=1), lambda x:x.update(clock_policy="FAKE"),
                 lambda x:x.update(fixture_count=9999), lambda x:x.update(fixture_sha256="D"*64),
                 lambda x:x.update(samples_per_case=19), lambda x:x.update(variant="release"),
                 lambda x:x["environment"].update(font_scale=float("inf")), lambda x:x["environment"].update(account="private"),
                 lambda x:x.update(frames=[dict(total_ns=10, first_draw=0, deadline_ns=20)])]
    for mutate in mutations:
        bad=copy.deepcopy(r);mutate(bad)
        try: analyze(bad, identity)
        except (ValueError, KeyError, TypeError): rejected += 1
        else: raise AssertionError("Accepted invalid evidence fixture")
    ui=copy.deepcopy(r);ui.update(metric="compose_fixture_dispatch_to_verified_visible_row",clock_policy="COMPOSE_AUTO_ADVANCE_DIAGNOSTIC",
        frames_scope="WHOLE_FIXTURE_INCLUDING_SETUP_WARMUPS",frames=[dict(total_ns=10,first_draw=0,deadline_ns=10) for _ in range(100)])
    assert analyze(ui,identity)["frames"]["diagnostic_deadline_miss_percent"]==100
    for key,value in [("frame_callbacks_dropped",1),("frames_truncated",True)]:
        bad=copy.deepcopy(ui);bad[key]=value;assert analyze(bad,identity)["frames"]["diagnostic_deadline_miss_percent"] is None
    bad=copy.deepcopy(ui);bad["frames"][0]["deadline_ns"]=-1;assert analyze(bad,identity)["frames"]["diagnostic_deadline_miss_percent"] is None
    after=copy.deepcopy(result);assert compare(result,after)["acceptance"]=="NOT_CERTIFIED"
    after["environment"]["sdk"]=35
    try: compare(result,after)
    except ValueError: rejected+=1
    else: raise AssertionError("Mixed device environments accepted")
    print(f"PASS NW140 evidence math/identity/sample/frame/environment fixtures; {rejected} rejected; all physical acceptance NOT_RUN")

def main():
    p=argparse.ArgumentParser();p.add_argument("--self-test",action="store_true");p.add_argument("--prepare",action="store_true")
    p.add_argument("--provenance");p.add_argument("--apk");p.add_argument("--test-apk");p.add_argument("--identity")
    p.add_argument("--analyze");p.add_argument("--compare",nargs=2,metavar=("BEFORE","AFTER"));p.add_argument("--before-identity");p.add_argument("--output")
    a=p.parse_args()
    try:
        require(sum(bool(x) for x in (a.self_test,a.prepare,a.analyze,a.compare))==1,"one operation")
        if a.self_test:self_test();return
        if a.prepare:
            require(all((a.provenance,a.apk,a.test_apk,a.output)),"prepare arguments");prepare(a.provenance,a.apk,a.test_apk,a.output)
            print("PREPARED_NOT_EXECUTED: current APK/test identity; all device/performance gates NOT_RUN");return
        require(a.identity and a.output,"identity and private output")
        result=analyze(read_json(a.analyze),read_json(a.identity)) if a.analyze else compare(
            analyze(read_json(a.compare[0]),read_json(a.before_identity)),analyze(read_json(a.compare[1]),read_json(a.identity)))
        out=private_path(a.output);out.parent.mkdir(parents=True,exist_ok=True)
        with out.open("x", encoding="utf-8") as stream: stream.write(json.dumps(result,indent=2,allow_nan=False)+"\n")
        print(result["status"]+": no app-wide acceptance or rating inferred")
    except (OSError,ValueError,KeyError,TypeError) as error:p.exit(1,"FAIL: "+str(error)+"\n")

if __name__=="__main__":main()
