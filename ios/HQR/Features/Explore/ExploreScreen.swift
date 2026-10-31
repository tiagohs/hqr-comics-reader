import Shared
import SwiftUI

/// SCR-EXP-001: the feed and the list of sources (RF-03, RF-25), with the search of every source on top (RF-23).
struct ExploreScreen: View {
    enum Tab: Hashable { case sources, feed }

    @EnvironmentObject private var navigator: Navigator
    @StateObject private var sources = ScreenModel(Core.graph.sourcesViewModel()) {
        Screen<SourcesViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @StateObject private var favorites = Favorites()
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
        .alert(L("feed_full_favorite"), isPresented: $favorites.feedFull) { Button(L("action_ok"), role: .cancel) {} }
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
            // RF-115: favorites first; they are the ones in the feed.
            let online = state.online.filter { $0.status == .on }
            let favored = online.filter { favorites.ids.contains(IosBridge.shared.sourceOfRow(row: $0)) }
            if !favored.isEmpty {
                Section {
                    ForEach(favored, id: \.name) { row in
                        sourceRow(id: IosBridge.shared.sourceOfRow(row: row), name: row.name, languages: row.languages, favorite: true)
                    }
                } header: {
                    sectionTitle(L("explore_favorites"), hint: L("explore_favorites_hint"))
                }
            }
            Section {
                ForEach(online.filter { !favorites.ids.contains(IosBridge.shared.sourceOfRow(row: $0)) }, id: \.name) { row in
                    sourceRow(id: IosBridge.shared.sourceOfRow(row: row), name: row.name, languages: row.languages, favorite: false)
                }
            } header: {
                sectionTitle(favored.isEmpty ? L("sources_online") : L("explore_other_sources"), hint: nil)
            }
            if let local = state.local {
                Section(L("sources_local")) {
                    sourceRow(id: IosBridge.shared.sourceOfRow(row: local), name: L("source_local"), languages: [], favorite: nil)
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

    private func sectionTitle(_ title: String, hint: String?) -> some View {
        HStack(alignment: .lastTextBaseline, spacing: 8) {
            Text(title).font(ComicFont.bangers(22, relativeTo: .headline)).foregroundStyle(Palette.onSurface)
            if let hint { Text(hint).font(.caption.weight(.semibold)).foregroundStyle(Palette.onSurfaceVariant) }
        }
        .textCase(nil)
    }

    /// A source with its monogram panel and, for online sources, the favorite star (RF-115).
    private func sourceRow(id: String, name: String, languages: [String], favorite: Bool?) -> some View {
        HStack(spacing: 12) {
            Button { navigator.open(.browse(sourceId: id, query: "")) } label: {
                HStack(spacing: 12) {
                    Text(String(name.prefix(2)).uppercased())
                        .font(ComicFont.bangers(20, relativeTo: .headline))
                        .frame(width: 44, height: 44)
                        .background(favorite == true ? AnyShapeStyle(.tint) : AnyShapeStyle(Palette.surface))
                        .foregroundStyle(favorite == true ? AnyShapeStyle(Color.white) : AnyShapeStyle(Palette.onSurface))
                        .overlay(Rectangle().stroke(Palette.onSurface, lineWidth: 2))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(name).font(ComicFont.bangers(20, relativeTo: .headline)).foregroundStyle(Palette.onSurface)
                        if !languages.isEmpty {
                            Text(languages.prefix(3).joined(separator: " · ").uppercased())
                                .font(.caption)
                                .foregroundStyle(Palette.onSurfaceVariant)
                        }
                    }
                    Spacer()
                }
            }
            .buttonStyle(.plain)
            if let favorite { FavoriteStar(name: name, on: favorite) { favorites.toggle(id) } }
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
                // RF-116: soft covers while the first answer comes.
                ScrollView {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 104), spacing: Metrics.spaceCoverGap)], spacing: 16) {
                        ForEach(0..<9, id: \.self) { _ in Skeleton().aspectRatio(2.0 / 3.0, contentMode: .fit) }
                    }
                    .padding(Metrics.spaceMarginCompact)
                }
                .scrollDisabled(true)
            } else if state.entries.isEmpty {
                EmptyState(title: L("feed_empty_title"), message: L("feed_empty_message"), systemImage: "newspaper")
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 20) {
                        // RF-116: how old what is on screen is, and that it is being asked again.
                        HStack(spacing: 8) {
                            if state.refreshing { DotsLoader() }
                            Text(L("feed_updated", ago(state.updatedAtEpochMs?.int64Value)))
                                .font(.caption.weight(.semibold)).foregroundStyle(Palette.onSurfaceVariant)
                        }
                        .padding(.horizontal, Metrics.spaceMarginCompact)
                        ForEach(Array(state.failed.keys), id: \.self) { origin in
                            ErrorBox(message: L("source_content_error", origin)).padding(.horizontal, Metrics.spaceMarginCompact)
                        }
                        ForEach(origins, id: \.self) { origin in
                            Text(origin).font(ComicFont.bangers(22, relativeTo: .headline)).padding(.horizontal, Metrics.spaceMarginCompact)
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
            navigator.openWork(id.int64Value, from: manga)
        }
    }
}

#Preview("ExploreScreen · Light") { ExploreScreen().previewScreen().preferredColorScheme(.light) }
#Preview("ExploreScreen · Dark") { ExploreScreen().previewScreen().preferredColorScheme(.dark) }
#Preview("FeedView · Light") { FeedView().previewScreen().preferredColorScheme(.light) }
#Preview("FeedView · Dark") { FeedView().previewScreen().preferredColorScheme(.dark) }
