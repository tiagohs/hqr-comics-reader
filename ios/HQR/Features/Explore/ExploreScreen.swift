import Shared
import SwiftUI

/// SCR-EXP-001: the feed and the list of sources (RF-03, RF-25), with the search of every source on top (RF-23).
struct ExploreScreen: View {
    enum Tab: Hashable { case sources, feed }

    @EnvironmentObject private var navigator: Navigator
    @StateObject private var sources = ScreenModel(Core.graph.sourcesViewModel()) {
        Screen<SourcesViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @State private var tab: Tab = .sources
    @State private var query = ""

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $tab) {
                Text(L("sources_tab")).tag(Tab.sources)
                Text(L("feed_tab")).tag(Tab.feed)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, Metrics.spaceMarginCompact)
            .padding(.vertical, 8)
            switch tab {
            case .sources: sourceList
            case .feed: FeedView()
            }
        }
        .background(Palette.background)
        .navigationTitle(L("nav_explore"))
        .searchable(text: $query, prompt: L("explore_search_all"))
        .onSubmit(of: .search) {
            let text = query.trimmingCharacters(in: .whitespaces)
            if !text.isEmpty { navigator.open(.search(query: text)) }
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { navigator.open(.sources) } label: {
                    Image(systemName: "slider.horizontal.3").accessibilityLabel(L("explore_manage_sources"))
                }
            }
        }
    }

    private var sourceList: some View {
        let state = sources.state
        return List {
            Section(L("sources_online")) {
                ForEach(state.online.filter { $0.status == .on }, id: \.name) { row in
                    sourceRow(id: IosBridge.shared.sourceOfRow(row: row), name: row.name, languages: row.languages)
                }
            }
            if let local = state.local {
                Section(L("sources_local")) {
                    sourceRow(id: IosBridge.shared.sourceOfRow(row: local), name: L("source_local"), languages: [])
                }
            }
            if !state.servers.isEmpty {
                Section(L("sources_servers")) {
                    ForEach(state.servers, id: \.url) { server in
                        let id = IosBridge.shared.sourceOfServer(server: server)
                        Button { navigator.open(.serverCatalog(serverId: id)) } label: {
                            Label(server.name, systemImage: "server.rack")
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .screenBackground()
    }

    private func sourceRow(id: String, name: String, languages: [String]) -> some View {
        Button { navigator.open(.browse(sourceId: id, query: "")) } label: {
            HStack {
                Text(name).foregroundStyle(Palette.onSurface)
                Spacer()
                if !languages.isEmpty {
                    Text(languages.prefix(3).joined(separator: " · ").uppercased())
                        .font(.caption)
                        .foregroundStyle(Palette.onSurfaceVariant)
                }
            }
        }
    }
}

/// The feed (RF-25): the latest of the saved searches the user pinned, by origin.
struct FeedView: View {
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var model = ScreenModel(Core.graph.feedViewModel()) {
        Screen<FeedViewModel.State>(viewModel: $0, flow: $0.state)
    }

    var body: some View {
        let state = model.state
        let origins = state.entries.reduce(into: [String]()) { list, entry in
            if !list.contains(entry.origin) { list.append(entry.origin) }
        }
        Group {
            if state.loading && state.entries.isEmpty {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if state.entries.isEmpty {
                EmptyState(title: L("feed_empty_title"), message: L("feed_empty_message"), systemImage: "newspaper")
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 20) {
                        ForEach(origins, id: \.self) { origin in
                            Text(origin).font(.headline).padding(.horizontal, Metrics.spaceMarginCompact)
                            ScrollView(.horizontal, showsIndicators: false) {
                                LazyHStack(alignment: .top, spacing: Metrics.spaceCoverGap) {
                                    ForEach(state.entries.filter { $0.origin == origin }, id: \.manga.listId) { entry in
                                        Button { open(entry.manga) } label: { CoverCard(manga: entry.manga).frame(width: 112) }
                                            .buttonStyle(.plain)
                                    }
                                }
                                .padding(.horizontal, Metrics.spaceMarginCompact)
                            }
                        }
                    }
                    .padding(.vertical, 8)
                }
                .refreshable { model.viewModel.refresh() }
            }
        }
    }

    private func open(_ manga: SourceManga) {
        launch {
            let id = try await model.viewModel.open(manga: manga)
            navigator.open(.manga(id.int64Value))
        }
    }
}

#Preview("ExploreScreen · Light") { ExploreScreen().previewScreen().preferredColorScheme(.light) }
#Preview("ExploreScreen · Dark") { ExploreScreen().previewScreen().preferredColorScheme(.dark) }
#Preview("FeedView · Light") { FeedView().previewScreen().preferredColorScheme(.light) }
#Preview("FeedView · Dark") { FeedView().previewScreen().preferredColorScheme(.dark) }
