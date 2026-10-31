import Shared
import SwiftUI

/// SCR-RDR-001: the pages of a chapter in the mode of the work (RF-36 to RF-41), the menu, the page bar, the end of
/// the chapter (SCR-RDR-003) and the reader options (SCR-RDR-002). The core keeps the progress and prefetches.
struct ReaderScreen: View {
    let target: ReaderTarget
    @Environment(\.dismiss) private var dismiss
    @Environment(\.horizontalSizeClass) private var sizeClass
    @StateObject private var model: ScreenModel<ReaderViewModel, ReaderViewModel.State>
    @State private var showSettings = false

    init(target: ReaderTarget) {
        self.target = target
        _model = StateObject(wrappedValue: ScreenModel(
            Core.graph.readerViewModel(mangaId: target.mangaId, chapterId: target.chapterId, page: target.page)
        ) { Screen<ReaderViewModel.State>(viewModel: $0, flow: $0.state) })
    }

    var body: some View {
        let state = model.state
        ZStack {
            background(state.settings).ignoresSafeArea()
            content(state)
            if state.menuVisible {
                menu(state)
            } else if state.settings.showPageNumber && !state.pages.isEmpty {
                // RF-48: the page number at the bottom while reading.
                Text(L("reader_page_of", Int(state.page) + 1, state.pages.count))
                    .font(.caption.monospacedDigit())
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(.black.opacity(0.5), in: Capsule())
                    .foregroundStyle(.white)
                    .frame(maxHeight: .infinity, alignment: .bottom)
                    .padding(.bottom, 8)
                    .allowsHitTesting(false)
            }
        }
        .statusBarHidden(!state.menuVisible)
        .persistentSystemOverlays(state.menuVisible ? .automatic : .hidden)
        .onAppear { UIApplication.shared.isIdleTimerDisabled = state.settings.keepScreenOn }
        .onChange(of: state.settings.keepScreenOn) { _, on in UIApplication.shared.isIdleTimerDisabled = on }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .sheet(isPresented: $showSettings) {
            ReaderSettingsSheet(settings: state.settings) { settings, onlyThisWork in
                model.viewModel.updateSettings(settings: settings, onlyThisWork: onlyThisWork)
            }
            .presentationDetents([.medium, .large])
        }
        .tint(Palette.primary)
    }

    @ViewBuilder
    private func content(_ state: ReaderViewModel.State) -> some View {
        if state.error != nil, let url = state.chapter?.source.externalUrl.flatMap(URL.init(string:)) {
            // RF-11: a chapter only on the publisher's site opens there.
            unavailable(L("manga_external"), url: url)
        } else if let error = state.error, state.pages.isEmpty {
            ErrorState(kind: error) { model.viewModel.retry() }.environment(\.colorScheme, .dark)
        } else if state.isEmptyChapter {
            // RF-51: the chapter's own page, else the work's.
            unavailable("", url: (state.chapter?.source.externalUrl ?? state.manga?.source.url).flatMap(URL.init(string:)))
        } else if state.loading && state.pages.isEmpty {
            ProgressView().tint(.white)
        } else if let manga = state.manga {
            switch state.settings.mode {
            case .vertical, .webtoon:
                VerticalPages(manga: manga, state: state, viewModel: model.viewModel)
            default:
                PagedPages(manga: manga, state: state, viewModel: model.viewModel, spread: sizeClass == .regular)
            }
        }
    }

    private func unavailable(_ message: String, url: URL?) -> some View {
        VStack(spacing: 16) {
            Text(L("reader_chapter_unavailable")).font(.title3.weight(.semibold)).multilineTextAlignment(.center)
            if !message.isEmpty { Text(message).foregroundStyle(.gray) }
            if let url {
                Button(L("action_open_browser")) { UIApplication.shared.open(url) }.buttonStyle(.borderedProminent)
            }
            Button(L("reader_back_to_work")) { dismiss() }.buttonStyle(.bordered)
        }
        .padding(32)
        .foregroundStyle(.white)
    }

    private func menu(_ state: ReaderViewModel.State) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Button { dismiss() } label: { Image(systemName: "xmark").font(.headline) }
                    .accessibilityLabel(L("action_back"))
                VStack(alignment: .leading, spacing: 2) {
                    Text(state.manga?.source.title ?? "").font(.headline).lineLimit(1)
                    if let chapter = state.chapter { Text(chapter.label).font(.caption).lineLimit(1) }
                }
                Spacer()
                Button { showSettings = true } label: { Image(systemName: "textformat.size") }
                    .accessibilityLabel(L("reader_settings"))
            }
            .padding()
            .background(.ultraThinMaterial)
            Spacer()
            VStack(spacing: 8) {
                if state.pages.count > 1 {
                    Slider(value: Binding(get: { Double(model.state.page) },
                                          set: { model.viewModel.onPageChanged(page: Int32($0.rounded())) }),
                           in: 0...Double(state.pages.count - 1), step: 1)
                }
                HStack {
                    Button { model.viewModel.goToPrevious() } label: { Image(systemName: "backward.end.fill") }
                        .disabled(state.previous == nil)
                        .accessibilityLabel(L("reader_previous_chapter"))
                    Spacer()
                    Text(L("reader_page_of", Int(state.page) + 1, state.pages.count)).font(.subheadline.monospacedDigit())
                    Spacer()
                    Button { model.viewModel.goToNext() } label: { Image(systemName: "forward.end.fill") }
                        .disabled(state.next == nil)
                        .accessibilityLabel(L("reader_next_chapter"))
                }
            }
            .padding()
            .background(.ultraThinMaterial)
        }
        .transition(.opacity)
    }

    private func background(_ settings: ReaderSettings) -> Color {
        switch settings.background {
        case .white: Palette.readerBackgroundWhite
        case .gray: Palette.readerBackgroundGray
        default: Palette.readerBackgroundBlack
        }
    }
}

/// Left to right and right to left, one screen at a time: a page, or two side by side on a wide iPad screen (RF-44,
/// laid out by the core's ReaderLayout), swiped like a book. The screen after the last is the end of the chapter.
private struct PagedPages: View {
    let manga: Manga
    let state: ReaderViewModel.State
    let viewModel: ReaderViewModel
    let spread: Bool
    @State private var selection = 0

    var body: some View {
        GeometryReader { geometry in
            let twoUp = spread && state.settings.doublePage && geometry.size.width > geometry.size.height
            // RF-44: in portrait, a wide page can show as two screens.
            let split = state.settings.splitWide && geometry.size.width <= geometry.size.height
            let views = ReaderLayout.shared.views(count: Int32(state.pages.count), wide: state.wide, double: twoUp, split: split)
            TabView(selection: $selection) {
                ForEach(0..<views.count, id: \.self) { index in
                    screen(views[index], size: geometry.size)
                        .contentShape(Rectangle())
                        .onTapGesture(coordinateSpace: .global) { point in tap(point, size: geometry.size, views: views) }
                        .tag(index)
                }
                ChapterEnd(state: state, viewModel: viewModel).tag(views.count)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .environment(\.layoutDirection, state.settings.mode == .rightToLeft ? .rightToLeft : .leftToRight)
            .onAppear { selection = screenOf(page: state.page, views: views) }
            .onChange(of: state.page) { _, page in
                // The core moved the page (slider, another chapter): follow it, unless the end is showing.
                if selection < views.count { selection = screenOf(page: page, views: views) }
            }
            .onChange(of: twoUp) { _, _ in selection = screenOf(page: state.page, views: views) }
            .onChange(of: selection) { _, index in
                if index >= views.count {
                    viewModel.onReachedEnd()
                } else if let first = views[index].pages.first?.int32Value, first != state.page {
                    viewModel.onPageChanged(page: first)
                }
            }
        }
        .ignoresSafeArea()
    }

    private func screenOf(page: Int32, views: [PageView]) -> Int {
        Int(ReaderLayout.shared.viewOf(views: views, page: page))
    }

    /// RF-38: the zones of the chosen layout, following the reading direction (TapZoneMap of the core).
    private func tap(_ point: CGPoint, size: CGSize, views: [PageView]) {
        let action = TapZoneMap.shared.action(
            zones: state.settings.tapZones,
            mode: state.settings.mode,
            invert: state.settings.invertZones,
            x: Float(point.x / max(size.width, 1)),
            y: Float(point.y / max(size.height, 1))
        )
        switch action {
        case .next: selection = min(selection + 1, views.count)
        case .previous: selection = max(selection - 1, 0)
        default: withAnimation { viewModel.toggleMenu() }
        }
    }

    @ViewBuilder
    private func screen(_ view: PageView, size: CGSize) -> some View {
        let pages = view.pages.map { Int($0.int32Value) }
        if pages.count == 2 {
            // The first page in reading order goes on the right when reading right to left.
            let ordered = ReaderLayout.shared.firstOnLeft(mode: state.settings.mode, invert: state.settings.invertPages) ? pages : pages.reversed()
            HStack(spacing: 0) {
                ForEach(ordered, id: \.self) { index in
                    PageImage(manga: manga, page: state.pages[index], maxPixel: size.height * 2, crop: state.settings.cropBorders)
                }
            }
            .environment(\.layoutDirection, .leftToRight)
        } else if let half = view as? PageViewHalf {
            // One half of a wide page, the half read first on the side the reading starts (RF-44).
            let leftFirst = ReaderLayout.shared.firstOnLeft(mode: state.settings.mode, invert: state.settings.invertPages)
            let showsLeft = (half.part == 0) == leftFirst
            PageImage(manga: manga, page: state.pages[Int(half.index)], maxPixel: size.height * 2, crop: state.settings.cropBorders)
                .frame(width: size.width * 2, height: size.height)
                .frame(width: size.width, height: size.height, alignment: showsLeft ? .leading : .trailing)
                .clipped()
                .environment(\.layoutDirection, .leftToRight)
        } else if let index = pages.first {
            ZoomablePage(manga: manga, page: state.pages[index], maxPixel: max(size.width, size.height) * 2, crop: state.settings.cropBorders)
                .environment(\.layoutDirection, .leftToRight)
        }
    }
}

/// Vertical and webtoon (RF-36): one long strip; webtoon without gaps. The page at the top of the screen is the
/// current one, and the strip opens at the saved page.
private struct VerticalPages: View {
    let manga: Manga
    let state: ReaderViewModel.State
    let viewModel: ReaderViewModel
    @State private var visible: Int?

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                LazyVStack(spacing: state.settings.mode == .webtoon ? 0 : 8) {
                    ForEach(0..<state.pages.count, id: \.self) { index in
                        PageImage(manga: manga, page: state.pages[index], maxPixel: 0, fitWidth: geometry.size.width * 2, crop: state.settings.cropBorders)
                            .id(index)
                    }
                    ChapterEnd(state: state, viewModel: viewModel)
                        .frame(minHeight: geometry.size.height * 0.8)
                        .id(state.pages.count)
                }
                .scrollTargetLayout()
            }
            .scrollPosition(id: $visible, anchor: .top)
            .onAppear { visible = Int(state.page) }
            .onChange(of: visible) { _, index in
                guard let index else { return }
                if index >= state.pages.count {
                    viewModel.onReachedEnd()
                } else if index != Int(state.page) {
                    viewModel.onPageChanged(page: Int32(index))
                }
            }
            .onTapGesture { withAnimation { viewModel.toggleMenu() } }
        }
        .ignoresSafeArea()
    }
}

/// One page decoded at the size it is shown (ImageIO), with "Try again" on failure (RF-41).
private struct PageImage: View {
    let manga: Manga
    let page: SourcePage
    let maxPixel: CGFloat
    /// For a strip shown at a width: the decode keeps that width, however tall the page is.
    var fitWidth: CGFloat? = nil
    /// RF-45: uniform margins cut before the page is drawn.
    var crop = false
    @StateObject private var loader = ImageLoader()
    @State private var attempt = 0

    var body: some View {
        Group {
            switch loader.phase {
            case .done(let image):
                Image(uiImage: image).resizable().scaledToFit()
            case .loading:
                ProgressView().tint(.gray).frame(maxWidth: .infinity, minHeight: 320)
            case .failed:
                VStack(spacing: 12) {
                    Text(L("reader_page_failed")).foregroundStyle(.gray)
                    Button(L("action_retry")) { attempt += 1 }.buttonStyle(.bordered)
                }
                .frame(maxWidth: .infinity, minHeight: 320)
            }
        }
        .task(id: "\(page.imageUrl)#\(attempt)#\(crop)") { await loader.load(.page(manga, page), maxPixel: maxPixel, fitWidth: fitWidth, crop: crop) }
        
    }
}

/// A page that zooms with a pinch or a double tap (RF-39) and goes back to fit when released at the original size.
private struct ZoomablePage: View {
    let manga: Manga
    let page: SourcePage
    let maxPixel: CGFloat
    var crop = false
    @State private var scale: CGFloat = 1
    @State private var lastScale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        PageImage(manga: manga, page: page, maxPixel: maxPixel, crop: crop)
            .scaleEffect(scale)
            .offset(offset)
            .gesture(
                MagnifyGesture()
                    .onChanged { value in scale = max(1, min(lastScale * value.magnification, 5)) }
                    .onEnded { _ in
                        lastScale = scale
                        if scale <= 1.01 { reset() }
                    }
            )
            .simultaneousGesture(
                DragGesture()
                    .onChanged { value in
                        guard scale > 1 else { return }
                        offset = CGSize(width: lastOffset.width + value.translation.width, height: lastOffset.height + value.translation.height)
                    }
                    .onEnded { _ in lastOffset = offset },
                including: scale > 1 ? .all : .subviews
            )
            .onTapGesture(count: 2) {
                withAnimation(.easeOut(duration: Metrics.motionFast)) {
                    if scale > 1 {
                        reset()
                    } else {
                        scale = 2
                        lastScale = 2
                    }
                }
            }
    }

    private func reset() {
        scale = 1
        lastScale = 1
        offset = .zero
        lastOffset = .zero
    }
}

/// SCR-RDR-003: "End of ch. 10", the next chapter (or none) and the gap warning (RF-42).
private struct ChapterEnd: View {
    let state: ReaderViewModel.State
    let viewModel: ReaderViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 16) {
            Text(L("reader_end")).font(.largeTitle.weight(.heavy))
            if let chapter = state.chapter {
                Text(L("reader_you_finished") + " · " + chapter.label).font(.subheadline)
            }
            if let next = state.next {
                Text(next.label).font(.headline)
                if let missing = state.transition?.missing, !missing.isEmpty {
                    Text(L("reader_gap", missing.map { "\($0)" }.joined(separator: ", ")))
                        .font(.subheadline).foregroundStyle(.orange)
                }
                Button(L("reader_read_next", next.numberText)) { viewModel.goToNext() }.buttonStyle(.borderedProminent)
            } else {
                Text(L("reader_no_next")).foregroundStyle(.gray)
            }
            Button(L("reader_back_to_work")) { dismiss() }.buttonStyle(.bordered)
        }
        .foregroundStyle(.white)
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview("ReaderScreen · Light") {
    ReaderScreen(target: ReaderTarget(mangaId: 1, chapterId: 1, page: 0)).previewEnvironment().preferredColorScheme(.light)
}
#Preview("ReaderScreen · Dark") {
    ReaderScreen(target: ReaderTarget(mangaId: 1, chapterId: 1, page: 0)).previewEnvironment().preferredColorScheme(.dark)
}
