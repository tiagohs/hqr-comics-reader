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
            if state.menuVisible { menu(state) }
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
        if let error = state.error, state.pages.isEmpty {
            ErrorState(kind: error) { model.viewModel.retry() }.environment(\.colorScheme, .dark)
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

/// Left to right and right to left, one page per screen (two on a wide iPad screen in landscape), swiped like a book.
/// The page after the last is the end of the chapter (SCR-RDR-003).
private struct PagedPages: View {
    let manga: Manga
    let state: ReaderViewModel.State
    let viewModel: ReaderViewModel
    let spread: Bool

    var body: some View {
        let count = state.pages.count
        GeometryReader { geometry in
            let twoUp = spread && state.settings.doublePage && geometry.size.width > geometry.size.height
            TabView(selection: Binding(
                get: { Int(state.page) },
                set: { index in
                    if index >= count {
                        viewModel.onReachedEnd()
                    } else {
                        viewModel.onPageChanged(page: Int32(index))
                    }
                }
            )) {
                ForEach(0..<count, id: \.self) { index in
                    page(index, twoUp: twoUp, size: geometry.size)
                        .contentShape(Rectangle())
                        .onTapGesture(coordinateSpace: .local) { point in tap(point, width: geometry.size.width, count: count) }
                        .tag(index)
                }
                ChapterEnd(state: state, viewModel: viewModel).tag(count)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .environment(\.layoutDirection, state.settings.mode == .rightToLeft ? .rightToLeft : .leftToRight)
        }
        .ignoresSafeArea()
    }

    /// RF-38: taps on the sides turn the page, in the middle show the menu; zones follow the reading direction.
    private func tap(_ point: CGPoint, width: CGFloat, count: Int) {
        let third = width / 3
        guard state.settings.tapZones != .off, point.x < third || point.x > third * 2 else {
            withAnimation { viewModel.toggleMenu() }
            return
        }
        var forward = point.x > third * 2
        if state.settings.mode == .rightToLeft { forward.toggle() }
        if state.settings.invertZones { forward.toggle() }
        let page = Int(state.page) + (forward ? 1 : -1)
        if page >= count {
            viewModel.onReachedEnd()
        } else if page >= 0 {
            viewModel.onPageChanged(page: Int32(page))
        }
    }

    @ViewBuilder
    private func page(_ index: Int, twoUp: Bool, size: CGSize) -> some View {
        if twoUp && index + 1 < state.pages.count && index % 2 == 0 {
            HStack(spacing: 0) {
                PageImage(manga: manga, page: state.pages[index + (state.settings.mode == .rightToLeft ? 1 : 0)], maxPixel: size.height * 2)
                PageImage(manga: manga, page: state.pages[index + (state.settings.mode == .rightToLeft ? 0 : 1)], maxPixel: size.height * 2)
            }
            .environment(\.layoutDirection, .leftToRight)
        } else {
            ZoomablePage(manga: manga, page: state.pages[index], maxPixel: max(size.width, size.height) * 2)
                .environment(\.layoutDirection, .leftToRight)
        }
    }
}

/// Vertical and webtoon (RF-36): one long strip; webtoon without gaps.
private struct VerticalPages: View {
    let manga: Manga
    let state: ReaderViewModel.State
    let viewModel: ReaderViewModel

    var body: some View {
        GeometryReader { geometry in
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: state.settings.mode == .webtoon ? 0 : 8) {
                        ForEach(0..<state.pages.count, id: \.self) { index in
                            PageImage(manga: manga, page: state.pages[index], maxPixel: geometry.size.width * 3)
                                .id(index)
                                .onAppear { viewModel.onPageChanged(page: Int32(index)) }
                        }
                        ChapterEnd(state: state, viewModel: viewModel)
                            .frame(minHeight: geometry.size.height * 0.8)
                            .onAppear { viewModel.onReachedEnd() }
                    }
                }
                .onAppear { proxy.scrollTo(Int(state.page), anchor: .top) }
                .onTapGesture { withAnimation { viewModel.toggleMenu() } }
            }
        }
        .ignoresSafeArea()
    }
}

/// One page decoded at the size it is shown (ImageIO), with "Try again" on failure (RF-41).
private struct PageImage: View {
    let manga: Manga
    let page: SourcePage
    let maxPixel: CGFloat
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
        .task(id: "\(page.imageUrl)#\(attempt)") { await loader.load(.page(manga, page), maxPixel: maxPixel) }
        .accessibilityLabel(L("reader_page_of", Int(page.index) + 1, 0))
    }
}

/// A page that zooms with a pinch or a double tap (RF-39) and goes back to fit when released at the original size.
private struct ZoomablePage: View {
    let manga: Manga
    let page: SourcePage
    let maxPixel: CGFloat
    @State private var scale: CGFloat = 1
    @State private var lastScale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        PageImage(manga: manga, page: page, maxPixel: maxPixel)
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
