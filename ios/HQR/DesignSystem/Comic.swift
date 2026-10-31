import Shared
import SwiftUI

// The comic language of ADR-026 on iOS: lettering, stamps, bursts, halftone, speed lines and soft loading.
// Controls (buttons, menus, lists, toggles) stay the system's (HIG).

enum ComicFont {
    /// Bangers for titles, chapter numbers and stamps; scales with Dynamic Type.
    static func bangers(_ size: CGFloat, relativeTo style: Font.TextStyle = .title) -> Font {
        .custom("Bangers-Regular", size: size, relativeTo: style)
    }

    /// Dela Gothic One (kana and common kanji) for the vertical original title of manga.
    static func japanese(_ size: CGFloat) -> Font { .custom("DelaGothicOne-Regular", fixedSize: size) }
}

/// A screen or work title: Bangers with the offset shadow in the tint.
struct ComicTitle: View {
    let text: String
    var size: CGFloat = 34

    var body: some View {
        Text(text)
            .font(ComicFont.bangers(size))
            .foregroundStyle(Palette.onSurface)
            .shadow(color: Color.accentColor.opacity(0.55), radius: 0, x: 2, y: 2)
            .lineLimit(4)
            .accessibilityAddTraits(.isHeader)
    }
}

/// "NOVO!" and other stamps: Bangers on the tint, tilted like a rubber stamp.
struct Stamp: View {
    let text: String

    var body: some View {
        Text(text)
            .font(ComicFont.bangers(15, relativeTo: .caption))
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .background(.tint)
            .foregroundStyle(.white)
            .rotationEffect(.degrees(-4))
    }
}

/// The comic explosion: 16 points.
struct BurstShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let points = 16
        for i in 0..<(points * 2) {
            let radius = i.isMultiple(of: 2) ? min(rect.width, rect.height) / 2 : min(rect.width, rect.height) * 0.36
            let angle = (Double(i) * 180 / Double(points) - 90) * .pi / 180
            let point = CGPoint(x: center.x + radius * cos(angle), y: center.y + radius * sin(angle))
            if i == 0 { path.move(to: point) } else { path.addLine(to: point) }
        }
        path.closeSubpath()
        return path
    }
}

/// A count or a sound ("+1", "OPS!") in a burst.
struct Burst: View {
    let text: String
    var size: CGFloat = 36
    var color: Color = .accentColor
    var textColor: Color = .white

    var body: some View {
        ZStack {
            BurstShape().fill(color)
            Text(text).font(ComicFont.bangers(size * 0.32, relativeTo: .caption)).foregroundStyle(textColor)
        }
        .frame(width: size, height: size)
    }
}

/// Halftone (Ben-Day) dots.
struct Halftone: View {
    var color: Color = Palette.onSurface.opacity(0.22)
    var step: CGFloat = 6
    var radius: CGFloat = 1.1

    var body: some View {
        Canvas { context, size in
            var row = 0
            var y = step / 2
            while y < size.height {
                var x = row.isMultiple(of: 2) ? step / 2 : step
                while x < size.width {
                    context.fill(Path(ellipseIn: CGRect(x: x - radius, y: y - radius, width: radius * 2, height: radius * 2)), with: .color(color))
                    x += step
                }
                y += step * 0.866
                row += 1
            }
        }
        .accessibilityHidden(true)
    }
}

/// Manga speed lines from the center, every 7 degrees.
struct SpeedLines: View {
    var color: Color = Palette.onSurface.opacity(0.3)

    var body: some View {
        Canvas { context, size in
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let reach = max(size.width, size.height)
            var angle = 0.0
            while angle < 360 {
                let rad = angle * .pi / 180
                var line = Path()
                line.move(to: CGPoint(x: center.x + reach * 0.34 * cos(rad), y: center.y + reach * 0.34 * sin(rad)))
                line.addLine(to: CGPoint(x: center.x + reach * cos(rad), y: center.y + reach * sin(rad)))
                context.stroke(line, with: .color(color), lineWidth: 1.2)
                angle += 7
            }
        }
        .accessibilityHidden(true)
    }
}

/// Work facts as small bordered tags: author (when known), status with its dot, and the source (SCR-OBR-001).
struct MetaTags: View {
    let author: String?
    let status: String?
    let statusColor: Color
    let source: String?
    var extra: [String] = []

    var body: some View {
        FlowLayout(spacing: 8) {
            if let author { tag(Label(author, systemImage: "person")) }
            if let status {
                tag(HStack(spacing: 6) {
                    Circle().fill(statusColor).frame(width: 8, height: 8)
                    Text(status)
                })
            }
            if let source { tag(Label(source, systemImage: "globe")) }
            ForEach(extra, id: \.self) { tag(Text($0)) }
        }
    }

    private func tag(_ content: some View) -> some View {
        content
            .font(.footnote.weight(.semibold))
            .labelStyle(.titleAndIcon)
            .padding(.horizontal, 10)
            .frame(minHeight: 32)
            .background(Palette.surface, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 8, style: .continuous).stroke(Palette.outline.opacity(0.6), lineWidth: 1))
            .foregroundStyle(Palette.onSurface)
    }
}

/// Wraps its children onto new lines, like Compose's FlowRow.
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, line: CGFloat = 0, widest: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > width {
                y += line + spacing
                x = 0
                line = 0
            }
            x += size.width + spacing
            line = max(line, size.height)
            widest = max(widest, x - spacing)
        }
        return CGSize(width: proposal.width ?? widest, height: y + line)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, line: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > bounds.minX && x + size.width > bounds.maxX {
                y += line + spacing
                x = bounds.minX
                line = 0
            }
            view.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            line = max(line, size.height)
        }
    }
}

/// Soft loading block: rounded, no border, a light sweeping slowly across (RF-117).
struct Skeleton: View {
    var cornerRadius: CGFloat = 6
    @State private var phase: CGFloat = -1
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(Palette.onSurface.opacity(0.08))
            .overlay {
                GeometryReader { geo in
                    LinearGradient(colors: [.clear, Palette.surface.opacity(0.75), .clear], startPoint: .leading, endPoint: .trailing)
                        .frame(width: geo.size.width)
                        .offset(x: geo.size.width * phase)
                }
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            }
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation(.easeInOut(duration: 1.6).repeatForever(autoreverses: false)) { phase = 1.2 }
            }
            .accessibilityHidden(true)
    }
}

/// Three dots pulsing in the tint, next to "Buscando no MangaDex…".
struct DotsLoader: View {
    @State private var on = false

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { i in
                Circle().fill(.tint).frame(width: 6, height: 6)
                    .opacity(on ? 1 : 0.25)
                    .animation(.easeInOut(duration: 0.6).repeatForever().delay(Double(i) * 0.15), value: on)
            }
        }
        .onAppear { on = true }
        .accessibilityHidden(true)
    }
}

/// The original title written top to bottom with an ink outline (manga only).
struct VerticalTitle: View {
    let text: String
    var size: CGFloat = 30

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(text.prefix(7).enumerated()), id: \.offset) { _, char in
                ZStack {
                    ForEach(0..<4, id: \.self) { i in
                        let offsets: [(CGFloat, CGFloat)] = [(-1.5, 0), (1.5, 0), (0, -1.5), (0, 1.5)]
                        Text(String(char)).font(ComicFont.japanese(size)).foregroundStyle(Palette.onSurface)
                            .offset(x: offsets[i].0, y: offsets[i].1)
                    }
                    Text(String(char)).font(ComicFont.japanese(size)).foregroundStyle(Palette.background)
                }
            }
        }
        .accessibilityHidden(true)
    }
}

extension View {
    /// A comic panel: paper, an ink frame and the ink shadow offset to the bottom right.
    func inkPanel(border: CGFloat = 3, shadow: CGFloat = 4) -> some View {
        background(Palette.surface)
            .overlay(Rectangle().stroke(Palette.onSurface, lineWidth: border))
            .background(Palette.onSurface.offset(x: shadow, y: shadow))
    }
}

extension String {
    /// Japanese text the bundled font can draw: kana or kanji, short.
    var isJapanese: Bool {
        !isEmpty && count <= 7 && unicodeScalars.allSatisfy {
            (0x3040...0x30FF).contains($0.value) || (0x4E00...0x9FFF).contains($0.value) || (0x3000...0x303F).contains($0.value)
        }
    }
}

/// RF-05: a failure that belongs to a source names it.
func sourceErrorMessage(_ kind: ErrorKind, source: String) -> String {
    switch kind {
    case .network, .server, .parse, .unknown, .notFound: return L("source_content_error", source)
    default: return kind.message
    }
}

#Preview("ComicTitle · Light") { ComicTitle(text: "Lâmina do Norte").padding().preferredColorScheme(.light) }
#Preview("ComicTitle · Dark") { ComicTitle(text: "Lâmina do Norte").padding().preferredColorScheme(.dark) }
#Preview("Stamp · Light") { Stamp(text: "NOVO!").padding().preferredColorScheme(.light) }
#Preview("Stamp · Dark") { Stamp(text: "NOVO!").padding().preferredColorScheme(.dark) }
#Preview("Burst · Light") { Burst(text: "+1").padding().preferredColorScheme(.light) }
#Preview("Burst · Dark") { Burst(text: "+1").padding().preferredColorScheme(.dark) }
#Preview("Halftone · Light") { Halftone().frame(width: 120, height: 80).preferredColorScheme(.light) }
#Preview("Halftone · Dark") { Halftone().frame(width: 120, height: 80).preferredColorScheme(.dark) }
#Preview("SpeedLines · Light") { SpeedLines().frame(width: 200, height: 200).preferredColorScheme(.light) }
#Preview("SpeedLines · Dark") { SpeedLines().frame(width: 200, height: 200).preferredColorScheme(.dark) }
#Preview("MetaTags · Light") {
    MetaTags(author: "Rui Takeda", status: "Em lançamento", statusColor: .green, source: "MangaDex").padding().preferredColorScheme(.light)
}
#Preview("MetaTags · Dark") {
    MetaTags(author: "Rui Takeda", status: "Em lançamento", statusColor: .green, source: "MangaDex").padding().preferredColorScheme(.dark)
}
#Preview("Skeleton · Light") { Skeleton().frame(width: 200, height: 14).padding().preferredColorScheme(.light) }
#Preview("Skeleton · Dark") { Skeleton().frame(width: 200, height: 14).padding().preferredColorScheme(.dark) }
#Preview("DotsLoader · Light") { DotsLoader().padding().preferredColorScheme(.light) }
#Preview("DotsLoader · Dark") { DotsLoader().padding().preferredColorScheme(.dark) }
#Preview("VerticalTitle · Light") { VerticalTitle(text: "北の刃").padding().preferredColorScheme(.light) }
#Preview("VerticalTitle · Dark") { VerticalTitle(text: "北の刃").padding().preferredColorScheme(.dark) }
