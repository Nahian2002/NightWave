"""Resource/caller regressions; never certify rendered accessibility or translation quality."""
from pathlib import Path
import argparse
import copy
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / 'app/src/main/java/com/nightwave/player/v2/ui'

def resources(folder):
    result = {}
    for path in (ROOT / 'app/src/main/res' / folder).glob('*.xml'):
        for node in ET.parse(path).getroot():
            if node.tag not in ('string', 'plurals'): continue
            name = node.attrib['name']
            assert name not in result, ('duplicate', name)
            result[name] = (node.tag, {item.attrib['quantity']: ''.join(item.itertext()) for item in node}
                            if node.tag == 'plurals' else ''.join(node.itertext()))
    return result

def formats(value):
    value = value.replace("%%", "")
    assert not re.search(r'%(?![1-9][0-9]*\$[sd]|%)', value), 'Unnumbered/invalid placeholder'
    return sorted(re.findall(r'%[1-9][0-9]*\$[sd]', value))

def verify(en, bn, source):
    assert en.keys() == bn.keys(), 'English/Bangla resource parity'
    assert len(en) >= 300, 'Primary resource catalog unexpectedly reduced'
    for body in source.values():
        for kind, key in re.findall(r'R\.(string|plurals)\.(nw141_\w+)', body):
            assert key in en, ('missing resource caller', key)
            assert en[key][0] == kind, ('resource kind', key)
    for key, (kind, value) in en.items():
        other_kind, translated = bn[key]
        assert kind == other_kind
        if kind == 'string':
            assert translated.strip(), key
            assert formats(value) == formats(translated), ('placeholder', key)
        else:
            assert 'one' in value and 'other' in value
            assert translated.keys() == value.keys()
            for quantity in value:
                assert translated[quantity].strip()
                assert formats(value[quantity]) == formats(translated[quantity])
    assert 'this.number(number.coerceAtLeast(1))' in source['NightWaveStrings.kt']
    assert 'title.ifBlank { get(R.string.nw141_untitled_track) }' in source['NightWaveStrings.kt']
    assert 'remember(context, configuration)' in source['NightWaveStrings.kt']
    for name in ('NightWaveStudioActivity.kt', 'StudioPlayerScreens.kt', 'NightWaveAccountHub.kt',
                 'NightWaveSoundStudio.kt', 'SoundStudioApplyBar.kt'):
        assert 'rememberNightWaveStrings()' in source[name], name
        assert 'strings.get(R.string.nw141_' in source[name], name
    player = source['StudioPlayerScreens.kt']
    assert 'strings.queueAnnouncement(' in player
    assert 'strings.get(R.string.nw141_remove_track, display.title)' in player
    assert 'title.take(36)' not in player
    assert 'strings.quantity(R.plurals.nw141_upcoming, upcoming.size)' in player
    assert 'strings.get(R.string.nw141_seek_position,' in player
    assert 'strings.get(R.string.nw141_cue_seek,' in player
    assert 'commitCallback(trackIdentity, mapped)' in source['NightWaveSeekTimeline.kt']
    assert 'contentDescription = strings.get(R.string.nw141_empty_permission)' in source['NightWaveSeekTimeline.kt']
    assert 'contentDescription = label' in source['NightWavePremiumChrome.kt']
    assert 'selected = active' in source['NightWavePremiumChrome.kt']
    assert 'heading()' in source['StudioComponents.kt']
    assert 'includeFontPadding = true' in source['NightWaveTheme.kt']
    assert 'LineHeightStyle.Trim.None' in source['NightWaveTheme.kt']
    assert 'letterSpacing = 0.sp' in source['NightWaveTheme.kt']
    assert 'LocalNightWaveReduceMotion provides reduceMotion' in source['NightWaveTheme.kt']

def main():
    parser = argparse.ArgumentParser(); parser.add_argument('--self-test', action='store_true'); args = parser.parse_args()
    en, bn = resources('values'), resources('values-bn')
    source = {p.name: p.read_text(encoding='utf-8') for p in UI.glob('*.kt')}
    verify(en, bn, source)
    if args.self_test:
        assert formats("%1$d%%") == ["%1$d"]
        def rejected(a, b, s):
            try: verify(a, b, s)
            except (AssertionError, ValueError): return
            raise AssertionError('Accepted corrupt localization fixture')
        broken = dict(bn); broken.pop('nw141_queue_play'); rejected(en, broken, source)
        broken_en = dict(en); broken_bn = dict(bn)
        broken_en.pop('nw141_songs'); broken_bn.pop('nw141_songs')
        rejected(broken_en, broken_bn, source)
        broken = copy.deepcopy(bn); broken['nw141_queue_play'] = ('string', '%2$s only'); rejected(en, broken, source)
        broken = copy.deepcopy(bn); broken['nw141_queue_play'] = ('string', ''); rejected(en, broken, source)
        broken = copy.deepcopy(bn); broken['nw141_tracks'] = ('plurals', {'other': '%1$s'}); rejected(en, broken, source)
        for name, marker in [('StudioPlayerScreens.kt', 'strings.queueAnnouncement('),
                             ('StudioPlayerScreens.kt', 'strings.get(R.string.nw141_remove_track, display.title)'),
                             ('NightWaveStrings.kt', 'remember(context, configuration)'),
                             ('NightWaveTheme.kt', 'LineHeightStyle.Trim.None'),
                             ('NightWavePremiumChrome.kt', 'selected = active'),
                             ('NightWaveSeekTimeline.kt', 'commitCallback(trackIdentity, mapped)')]:
            broken = dict(source); broken[name] = broken[name].replace(marker, 'BROKEN_FIXTURE'); rejected(en, bn, broken)
    print('PASS NW141 resource parity/placeholders/quantities, full Unicode announcements, active locale and actual callers; physical NOT_RUN')

if __name__ == '__main__': main()
