import SwiftUI

/// RF-118, design.md §4: the comic motion, the same timings as Android (ComicMotion.kt). Every movement stays between
/// 150 and 480 ms; with "Reduce Motion" everything becomes a 150 ms fade.
enum ComicMotion {
    static let fade = 0.15
    static let panel = 0.42
    static let cascade = 0.24
    static let sketch = 0.46
    static let cascadeStep = 0.03
    static let cascadeRows = 8
    /// After this long a screen is open: rows that scroll in later just show.
    static let opening = 0.7

    static func cascadeDelay(_ index: Int, reduce: Bool) -> Double {
        reduce ? 0 : Double(min(max(index, 0), cascadeRows)) * cascadeStep
    }

    static func duration(_ seconds: Double, reduce: Bool) -> Double { reduce ? fade : seconds }

    /// The whole vertical title takes at most 480 ms, however many letters.
    static func writeSeconds(_ count: Int) -> Double { min(0.14 + Double(max(count, 1)) * 0.045, 0.48) }

    /// How visible the `index`-th letter is when the title is `progress` (0 to 1) written.
    static func letterAlpha(index: Int, count: Int, progress: Double, reduce: Bool) -> Double {
        if reduce { return 1 }
        let total = writeSeconds(count)
        let start = Double(index) * 0.045 * (total / (0.14 + Double(count) * 0.045))
        return min(max((progress * total - start) / 0.14, 0), 1)
    }
}

/// Where the covers fly from (the zoom into the work, iOS 18 and later).
private struct CoverNamespaceKey: EnvironmentKey {
    static let defaultValue: Namespace.ID? = nil
}

extension EnvironmentValues {
    var coverNamespace: Namespace.ID? {
        get { self[CoverNamespaceKey.self] }
        set { self[CoverNamespaceKey.self] = newValue }
    }
}

/// The id a cover and its work screen share: the source and the work's key.
func coverTransitionId(sourceId: String, key: String) -> String { "cover/\(sourceId)/\(key)" }

extension View {
    /// RF-118: this cover is where the work screen zooms from.
    @ViewBuilder func coverSource(_ id: String) -> some View {
        modifier(CoverSource(id: id))
    }

    /// RF-118: the work screen grows out of the cover that was tapped.
    @ViewBuilder func zoomFromCover(_ id: String?) -> some View {
        modifier(ZoomFromCover(id: id))
    }

    /// "NOVO!" lands like a rubber stamp: big and tilted, then pressed into place.
    func stampIn() -> some View { modifier(StampIn()) }

    /// Lists cascade only while the screen opens (`since`).
    func cascadeIn(_ index: Int, since: Date) -> some View { modifier(CascadeIn(index: index, since: since)) }
}

private struct CoverSource: ViewModifier {
    let id: String
    @Environment(\.coverNamespace) private var namespace

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *), let namespace {
            content.matchedTransitionSource(id: id, in: namespace)
        } else {
            content
        }
    }
}

private struct ZoomFromCover: ViewModifier {
    let id: String?
    @Environment(\.coverNamespace) private var namespace
    @Environment(\.accessibilityReduceMotion) private var reduce

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *), let namespace, let id, !reduce {
            content.navigationTransition(.zoom(sourceID: id, in: namespace))
        } else {
            content
        }
    }
}

private struct StampIn: ViewModifier {
    @Environment(\.accessibilityReduceMotion) private var reduce
    @State private var pressed = false

    func body(content: Content) -> some View {
        let done = pressed || reduce
        content
            .scaleEffect(done ? 1 : 1.9)
            .rotationEffect(.degrees(done ? 0 : -10))
            .opacity(done ? 1 : 0)
            .onAppear {
                guard !reduce else { return }
                withAnimation(.spring(response: 0.28, dampingFraction: 0.55)) { pressed = true }
            }
    }
}

private struct CascadeIn: ViewModifier {
    let index: Int
    @Environment(\.accessibilityReduceMotion) private var reduce
    @State private var shown: Bool

    init(index: Int, since: Date) {
        self.index = index
        // Rows that come in after the screen opened just show.
        _shown = State(initialValue: Date().timeIntervalSince(since) >= ComicMotion.opening)
    }

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(y: shown ? 0 : 14)
            .onAppear {
                guard !shown else { return }
                if reduce {
                    shown = true
                } else {
                    withAnimation(.easeOut(duration: ComicMotion.cascade).delay(ComicMotion.cascadeDelay(index, reduce: false))) {
                        shown = true
                    }
                }
            }
    }
}

/// 0 to 1 once, when the view first shows; at once with "Reduce Motion".
struct Entrance: ViewModifier {
    var duration = ComicMotion.panel
    var delay = 0.0
    @Binding var progress: Double
    @Environment(\.accessibilityReduceMotion) private var reduce

    func body(content: Content) -> some View {
        content.onAppear {
            guard progress < 1 else { return }
            if reduce {
                progress = 1
            } else {
                withAnimation(.easeOut(duration: duration).delay(delay)) { progress = 1 }
            }
        }
    }
}

/// A frame drawn by hand: four loose strokes whose corners run past each other (the first-run cards).
struct SketchFrame: Shape {
    var wobble: CGFloat = 0

    func path(in rect: CGRect) -> Path {
        let over: CGFloat = 6
        var path = Path()
        path.move(to: CGPoint(x: rect.minX - over + wobble, y: rect.minY - wobble))
        path.addLine(to: CGPoint(x: rect.maxX + over * 0.4 + wobble, y: rect.minY - wobble))
        path.move(to: CGPoint(x: rect.maxX + wobble, y: rect.minY - over - wobble))
        path.addLine(to: CGPoint(x: rect.maxX + wobble, y: rect.maxY + over * 0.5 - wobble))
        path.move(to: CGPoint(x: rect.maxX + over + wobble, y: rect.maxY - wobble))
        path.addLine(to: CGPoint(x: rect.minX - over * 0.3 + wobble, y: rect.maxY - wobble))
        path.move(to: CGPoint(x: rect.minX + wobble, y: rect.maxY + over - wobble))
        path.addLine(to: CGPoint(x: rect.minX + wobble, y: rect.minY - over * 0.6 - wobble))
        return path
    }
}

#Preview("SketchFrame · Light") {
    SketchFrame().trim(from: 0, to: 0.8).stroke(.black, lineWidth: 2).frame(width: 200, height: 80).padding().preferredColorScheme(.light)
}
#Preview("SketchFrame · Dark") {
    SketchFrame().trim(from: 0, to: 0.8).stroke(.white, lineWidth: 2).frame(width: 200, height: 80).padding().preferredColorScheme(.dark)
}
