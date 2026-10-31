import Shared
import SwiftUI

/// Every screen past a tab's root (the SCR ids of docs/ux-foundations.md). Ids cross as plain values.
enum Route: Hashable {
    case manga(Int64)
    case browse(sourceId: String, query: String)
    case search(query: String)
    case migrate(Int64)
    case categories
    case downloads
    case sources
    case addServer(serverId: String)
    case serverCatalog(serverId: String)
    case backup
    case about
    case challenge(url: String)
}

/// The reader opens over everything (full screen), from any tab.
struct ReaderTarget: Identifiable, Hashable {
    let mangaId: Int64
    let chapterId: Int64
    let page: Int32
    var id: String { "\(mangaId)/\(chapterId)/\(page)" }
}

/// The reader over the whole app: any tab opens it, the root presents it full screen.
@MainActor
final class ReaderPresenter: ObservableObject {
    @Published var target: ReaderTarget?
}

/// What a screen uses to move around: push in its own stack or open the reader.
@MainActor
final class Navigator: ObservableObject {
    @Published var path = NavigationPath()
    let presenter: ReaderPresenter

    init(presenter: ReaderPresenter) {
        self.presenter = presenter
    }

    func open(_ route: Route) { path.append(route) }

    func back() {
        if !path.isEmpty { path.removeLast() }
    }

    func read(mangaId: Int64, chapterId: Int64, page: Int32 = 0) {
        presenter.target = ReaderTarget(mangaId: mangaId, chapterId: chapterId, page: page)
    }
}

/// One tab's navigation stack, with its own Navigator.
struct TabStack<Root: View>: View {
    @StateObject private var navigator: Navigator
    private let root: Root

    init(presenter: ReaderPresenter, @ViewBuilder root: () -> Root) {
        _navigator = StateObject(wrappedValue: Navigator(presenter: presenter))
        self.root = root()
    }

    var body: some View {
        NavigationStack(path: $navigator.path) {
            root.routes()
        }
        .environmentObject(navigator)
    }
}

extension View {
    /// The screens every stack can show.
    func routes() -> some View {
        navigationDestination(for: Route.self) { route in
            switch route {
            case .manga(let id): MangaScreen(mangaId: id)
            case .browse(let sourceId, let query): BrowseSourceScreen(sourceId: sourceId, query: query)
            case .search(let query): GlobalSearchScreen(query: query)
            case .migrate(let id): MigrationScreen(mangaId: id)
            case .categories: CategoriesScreen()
            case .downloads: DownloadsScreen()
            case .sources: SourcesScreen()
            case .addServer(let id): AddServerScreen(serverId: id)
            case .serverCatalog(let id): ServerCatalogScreen(serverId: id)
            case .backup: BackupScreen()
            case .about: AboutScreen()
            case .challenge(let url): ChallengeScreen(url: url)
            }
        }
    }
}

#Preview("TabStack · Light") { TabStack(presenter: ReaderPresenter()) { Text(L("nav_library")) }.preferredColorScheme(.light) }
#Preview("TabStack · Dark") { TabStack(presenter: ReaderPresenter()) { Text(L("nav_library")) }.preferredColorScheme(.dark) }
