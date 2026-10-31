import Shared
import SwiftUI

/// The four destinations of the app (ADR-012): a tab bar on the iPhone, a sidebar on the iPad.
enum Destination: String, CaseIterable, Identifiable {
    case library, explore, history, settings
    var id: String { rawValue }

    var title: String {
        switch self {
        case .library: L("nav_library")
        case .explore: L("nav_explore")
        case .history: L("nav_history")
        case .settings: L("nav_settings")
        }
    }

    var systemImage: String {
        switch self {
        case .library: "books.vertical"
        case .explore: "safari"
        case .history: "clock.arrow.circlepath"
        case .settings: "gearshape"
        }
    }

    @MainActor @ViewBuilder var root: some View {
        switch self {
        case .library: LibraryScreen()
        case .explore: ExploreScreen()
        case .history: HistoryScreen()
        case .settings: SettingsScreen()
        }
    }
}

struct RootView: View {
    @Environment(\.horizontalSizeClass) private var sizeClass
    @StateObject private var presenter = ReaderPresenter()
    @StateObject private var appearance = Appearance()
    @State private var selection: Destination = .library

    var body: some View {
        Group {
            if sizeClass == .regular {
                NavigationSplitView {
                    List(Destination.allCases, selection: Binding($selection)) { destination in
                        Label(destination.title, systemImage: destination.systemImage).tag(destination)
                    }
                    .navigationTitle(L("app_name"))
                } detail: {
                    TabStack(presenter: presenter) { selection.root }.id(selection)
                }
            } else {
                TabView(selection: $selection) {
                    ForEach(Destination.allCases) { destination in
                        TabStack(presenter: presenter) { destination.root }
                            .tabItem { Label(destination.title, systemImage: destination.systemImage) }
                            .tag(destination)
                    }
                }
            }
        }
        .environmentObject(presenter)
        .environmentObject(appearance)
        .preferredColorScheme(appearance.colorScheme)
        .tint(Palette.primary)
        .fullScreenCover(item: $presenter.target) { target in
            ReaderScreen(target: target)
        }
        .overlay(alignment: .top) { OfflineBanner() }
    }
}

/// RF-87: a discreet banner while there is no network.
struct OfflineBanner: View {
    @StateObject private var online = FlowModel<KotlinBoolean>(Core.graph.connectivity().online)

    var body: some View {
        if !online.value.boolValue {
            Text(L("error_offline_banner"))
                .font(.footnote.weight(.medium))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Palette.surfaceHighest, in: Capsule())
                .foregroundStyle(Palette.onSurface)
                .padding(.top, 4)
                .transition(.move(edge: .top).combined(with: .opacity))
        }
    }
}

/// The theme the user chose (RF-76): system, light or dark.
@MainActor
final class Appearance: ObservableObject {
    @Published var theme: ThemeMode

    init() {
        theme = (Core.graph.preferences().theme.value as? ThemeMode) ?? .system
    }

    var colorScheme: ColorScheme? {
        switch theme {
        case .light: .light
        case .dark: .dark
        default: nil
        }
    }

    func set(_ mode: ThemeMode) {
        Core.graph.preferences().setTheme(mode: mode)
        theme = mode
    }
}

#Preview("RootView · Light") { RootView().preferredColorScheme(.light) }
#Preview("RootView · Dark") { RootView().preferredColorScheme(.dark) }
#Preview("OfflineBanner · Light") { OfflineBanner().previewEnvironment().preferredColorScheme(.light) }
#Preview("OfflineBanner · Dark") { OfflineBanner().previewEnvironment().preferredColorScheme(.dark) }
