import Shared
import SwiftUI

/// SCR-SRC-002: a source's library area (RF-128) and, for MangaDex, chapter languages, image quality and suggestive
/// content (RF-06, RF-08).
struct SourceSettingsScreen: View {
    var sourceId = "mangadex"
    private let prefs = Core.graph.preferences()
    @State private var override: Set<String>?
    @State private var dataSaver = false
    @State private var shelf: Shelf?

    var body: some View {
        Form {
            // RF-128: where this source's works go in the library.
            Section {
                Picker(L("source_settings_shelf_label"), selection: Binding(
                    get: { shelf ?? IosBridge.shared.sourceShelf(sourceId: sourceId, shelves: prefs.sourceShelves.value as? [String: Shelf] ?? [:]) },
                    set: { value in
                        shelf = value
                        IosBridge.shared.setSourceShelf(prefs: prefs, sourceId: sourceId, shelf: value)
                    }
                )) {
                    Text(L("library_area_comics")).tag(Shelf.comics)
                    Text(L("library_area_manga")).tag(Shelf.manga)
                }
                .pickerStyle(.segmented)
            } header: {
                Text(L("source_settings_shelf_label"))
            } footer: {
                Text(L("source_settings_shelf_hint"))
            }
            if sourceId == "mangadex" { mangaDex }
        }
        .navigationTitle(sourceDisplayName(sourceId))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            override = (prefs.mangaDexLanguagesOverride.value).map { Self.set($0) }
            dataSaver = (prefs.mangaDexDataSaver.value as? KotlinBoolean)?.boolValue ?? false
        }
    }

    @ViewBuilder
    private var mangaDex: some View {
            Section(L("source_settings_languages")) {
                Toggle(isOn: Binding(get: { override == nil }, set: { follow in
                    let next: Set<String>? = follow ? nil : Self.set(prefs.contentLanguages.value)
                    override = next
                    prefs.setMangaDexLanguages(languages: next)
                })) {
                    Text(L("source_settings_follow"))
                    Text(L("source_settings_follow_hint"))
                }
                ForEach(ContentLanguageKt.CONTENT_LANGUAGES, id: \.label) { language in
                    let codes = Set(language.codes)
                    let chosen = override ?? Self.set(prefs.contentLanguages.value)
                    Toggle(language.label, isOn: Binding(
                        get: { !chosen.isDisjoint(with: codes) },
                        set: { on in
                            let next = on ? chosen.union(codes) : chosen.subtracting(codes)
                            guard !next.isEmpty else { return }
                            override = next
                            prefs.setMangaDexLanguages(languages: next)
                        }
                    ))
                }
            }
            Section {
                Picker(L("source_settings_quality"), selection: Binding(get: { dataSaver }, set: { saver in
                    dataSaver = saver
                    prefs.setMangaDexDataSaver(on: saver)
                })) {
                    Text(L("source_settings_quality_original")).tag(false)
                    Text(L("source_settings_quality_saver")).tag(true)
                }
                .pickerStyle(.segmented)
            } header: {
                Text(L("source_settings_quality"))
            } footer: {
                Text(L("source_settings_quality_hint"))
            }
            Section(L("source_settings_content")) {
                SuggestiveToggle(title: L("source_settings_suggestive"))
            }
    }

    static func set(_ value: Any?) -> Set<String> {
        (value as? Set<String>) ?? Set((value as? NSSet)?.compactMap { $0 as? String } ?? [])
    }
}

/// RF-08: MangaDex's suggestive content, turned on only after the 18+ confirmation (Settings and SCR-SRC-002).
struct SuggestiveToggle: View {
    let title: String
    private let prefs = Core.graph.preferences()
    @State private var on = (Core.graph.preferences().mangaDexSuggestive.value as? KotlinBoolean)?.boolValue ?? false
    @State private var askAge = false

    var body: some View {
        Toggle(isOn: Binding(get: { on }, set: { wants in
            if wants {
                askAge = true
            } else {
                on = false
                prefs.setMangaDexSuggestive(on: false)
            }
        })) {
            Text(title)
            Text(L("settings_mangadex_suggestive_hint"))
        }
        .alert(L("settings_adult_title"), isPresented: $askAge) {
            Button(L("settings_adult_confirm")) {
                on = true
                prefs.setMangaDexSuggestive(on: true)
            }
            Button(L("action_cancel"), role: .cancel) {}
        } message: {
            Text(L("settings_adult_message"))
        }
    }
}

#Preview("SourceSettingsScreen · Light") { SourceSettingsScreen().previewScreen().preferredColorScheme(.light) }
#Preview("SourceSettingsScreen · Dark") { SourceSettingsScreen().previewScreen().preferredColorScheme(.dark) }
#Preview("SuggestiveToggle · Light") { Form { SuggestiveToggle(title: L("settings_mangadex_suggestive")) }.preferredColorScheme(.light) }
#Preview("SuggestiveToggle · Dark") { Form { SuggestiveToggle(title: L("settings_mangadex_suggestive")) }.preferredColorScheme(.dark) }
