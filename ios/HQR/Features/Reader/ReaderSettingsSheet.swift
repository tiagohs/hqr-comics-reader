import Shared
import SwiftUI

/// SCR-RDR-002: reading mode, scale, tap zones, background and the reader's own options, for this work or for all
/// (RF-36 to RF-39, RF-44 to RF-50).
struct ReaderSettingsSheet: View {
    @State var settings: ReaderSettings
    let apply: (ReaderSettings, Bool) -> Void
    @State private var onlyThisWork = true
    private let bridge = IosBridge.shared

    var body: some View {
        NavigationStack {
            Form {
                Toggle(L("reader_only_this_work"), isOn: $onlyThisWork)
                Picker(L("reader_mode"), selection: binding(\.mode) { bridge.changeReaderSettings(settings: settings, mode: $0, scale: nil, background: nil, tapZones: nil, doublePage: nil, keepScreenOn: nil, showPageNumber: nil) }) {
                    Text(L("reader_mode_ltr")).tag(ReaderMode.leftToRight)
                    Text(L("reader_mode_rtl")).tag(ReaderMode.rightToLeft)
                    Text(L("reader_mode_vertical")).tag(ReaderMode.vertical)
                    Text(L("reader_mode_webtoon")).tag(ReaderMode.webtoon)
                }
                Picker(L("reader_scale"), selection: binding(\.scale) { bridge.changeReaderSettings(settings: settings, mode: nil, scale: $0, background: nil, tapZones: nil, doublePage: nil, keepScreenOn: nil, showPageNumber: nil) }) {
                    Text(L("reader_scale_screen")).tag(PageScale.fitScreen)
                    Text(L("reader_scale_width")).tag(PageScale.fitWidth)
                    Text(L("reader_scale_height")).tag(PageScale.fitHeight)
                    Text(L("reader_scale_original")).tag(PageScale.original)
                }
                Picker(L("reader_zones"), selection: binding(\.tapZones) { bridge.changeReaderSettings(settings: settings, mode: nil, scale: nil, background: nil, tapZones: $0, doublePage: nil, keepScreenOn: nil, showPageNumber: nil) }) {
                    Text(L("reader_zones_default")).tag(TapZones.default_)
                    Text(L("reader_zones_kindle")).tag(TapZones.kindle)
                    Text(L("reader_zones_edges")).tag(TapZones.edges)
                    Text(L("reader_zones_sides")).tag(TapZones.sides)
                    Text(L("reader_zones_off")).tag(TapZones.off)
                }
                Picker(L("reader_background"), selection: binding(\.background) { bridge.changeReaderSettings(settings: settings, mode: nil, scale: nil, background: $0, tapZones: nil, doublePage: nil, keepScreenOn: nil, showPageNumber: nil) }) {
                    Text(L("reader_bg_black")).tag(ReaderBackground.black)
                    Text(L("reader_bg_gray")).tag(ReaderBackground.gray)
                    Text(L("reader_bg_white")).tag(ReaderBackground.white)
                }
                Section {
                    Toggle(isOn: flag(\.doublePage) { bridge.changeReaderSettings(settings: settings, mode: nil, scale: nil, background: nil, tapZones: nil, doublePage: KotlinBoolean(value: $0), keepScreenOn: nil, showPageNumber: nil) }) {
                        Text(L("reader_double"))
                        Text(L("reader_double_hint"))
                    }
                    Toggle(isOn: flag(\.keepScreenOn) { bridge.changeReaderSettings(settings: settings, mode: nil, scale: nil, background: nil, tapZones: nil, doublePage: nil, keepScreenOn: KotlinBoolean(value: $0), showPageNumber: nil) }) {
                        Text(L("reader_keep_on"))
                        Text(L("reader_keep_on_hint"))
                    }
                    Toggle(isOn: flag(\.showPageNumber) { bridge.changeReaderSettings(settings: settings, mode: nil, scale: nil, background: nil, tapZones: nil, doublePage: nil, keepScreenOn: nil, showPageNumber: KotlinBoolean(value: $0)) }) {
                        Text(L("reader_page_number"))
                        Text(L("reader_page_number_hint"))
                    }
                }
            }
            .navigationTitle(L("reader_settings"))
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func binding<Value>(_ path: KeyPath<ReaderSettings, Value>, _ change: @escaping (Value) -> ReaderSettings) -> Binding<Value> {
        Binding(get: { settings[keyPath: path] }, set: { value in
            settings = change(value)
            apply(settings, onlyThisWork)
        })
    }

    private func flag(_ path: KeyPath<ReaderSettings, Bool>, _ change: @escaping (Bool) -> ReaderSettings) -> Binding<Bool> {
        binding(path, change)
    }
}

#Preview("ReaderSettingsSheet · Light") {
    ReaderSettingsSheet(settings: (Core.graph.readerViewModel(mangaId: 1, chapterId: 1, page: 0).state.value as! ReaderViewModel.State)
        .settings, apply: { _, _ in }).previewEnvironment().preferredColorScheme(.light)
}
#Preview("ReaderSettingsSheet · Dark") {
    ReaderSettingsSheet(settings: (Core.graph.readerViewModel(mangaId: 1, chapterId: 1, page: 0).state.value as! ReaderViewModel.State)
        .settings, apply: { _, _ in }).previewEnvironment().preferredColorScheme(.dark)
}
