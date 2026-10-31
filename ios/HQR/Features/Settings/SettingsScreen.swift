import Shared
import SwiftUI

/// SCR-SET-001: appearance, content languages, sources, downloads, backup, cache and about (RF-76, RF-77, RF-80,
/// RF-81, RF-98). The app language follows the iPhone's per-app language setting.
struct SettingsScreen: View {
    @EnvironmentObject private var navigator: Navigator
    @EnvironmentObject private var appearance: Appearance
    @State private var coverColor = (Core.graph.preferences().coverColor.value as? KotlinBoolean)?.boolValue ?? true
    @State private var languages = Self.contentLanguages()
    @State private var cacheBytes: Int64?
    private let prefs = Core.graph.preferences()

    var body: some View {
        List {
            Section(L("settings_appearance")) {
                Picker(L("settings_appearance"), selection: Binding(get: { appearance.theme }, set: { appearance.set($0) })) {
                    Text(L("settings_theme_system")).tag(ThemeMode.system)
                    Text(L("settings_theme_light")).tag(ThemeMode.light)
                    Text(L("settings_theme_dark")).tag(ThemeMode.dark)
                }
                Toggle(isOn: Binding(get: { coverColor }, set: { on in
                    coverColor = on
                    prefs.setCoverColor(on: on)
                })) {
                    Text(L("settings_cover_color"))
                    Text(L("settings_cover_color_hint"))
                }
                Button(L("settings_language")) {
                    if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                }
            }
            Section(L("settings_content_languages")) {
                ForEach(ContentLanguageKt.CONTENT_LANGUAGES, id: \.label) { language in
                    let codes = Set(language.codes)
                    Toggle(language.label, isOn: Binding(
                        get: { !languages.isDisjoint(with: codes) },
                        set: { on in
                            let next = on ? languages.union(codes) : languages.subtracting(codes)
                            guard !next.isEmpty else { return }
                            languages = next
                            prefs.setContentLanguages(languages: next)
                        }
                    ))
                }
            }
            Section {
                NavigationLink(value: Route.sources) { Label(L("settings_manage_sources"), systemImage: "square.stack.3d.up") }
                NavigationLink(value: Route.categories) { Label(L("library_categories"), systemImage: "folder") }
                NavigationLink(value: Route.downloads) { Label(L("settings_downloads"), systemImage: "arrow.down.circle") }
                NavigationLink(value: Route.backup) { Label(L("settings_backup"), systemImage: "externaldrive") }
            }
            Section(L("settings_cache")) {
                Text(L("settings_cache_size", cacheBytes.map { ByteCountFormatter.string(fromByteCount: $0, countStyle: .file) } ?? "…"))
                    .foregroundStyle(Palette.onSurfaceVariant)
                Button(L("settings_cache_clear"), role: .destructive) {
                    launch {
                        try await Core.graph.imageCache().clear()
                        await loadCacheSize()
                    }
                }
            }
            Section(L("settings_other")) {
                NavigationLink(value: Route.about) { Label(L("settings_about"), systemImage: "info.circle") }
            }
        }
        .listStyle(.insetGrouped)
        .screenBackground()
        .navigationTitle(L("nav_settings"))
        .task { await loadCacheSize() }
    }

    private func loadCacheSize() async {
        cacheBytes = (try? await Core.graph.imageCache().size())?.int64Value
    }

    private static func contentLanguages() -> Set<String> {
        let value = Core.graph.preferences().contentLanguages.value
        return (value as? Set<String>) ?? Set((value as? NSSet)?.compactMap { $0 as? String } ?? [])
    }
}

#Preview("SettingsScreen · Light") {
    SettingsScreen().previewScreen().environmentObject(Appearance()).preferredColorScheme(.light)
}
#Preview("SettingsScreen · Dark") {
    SettingsScreen().previewScreen().environmentObject(Appearance()).preferredColorScheme(.dark)
}
