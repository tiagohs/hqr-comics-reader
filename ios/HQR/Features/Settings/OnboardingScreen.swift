import Shared
import SwiftUI

/// SCR-ONB-001 (RF-81): how the person likes to read, the content languages and the notice that HQR hosts nothing,
/// once, before anything else. Drawn like a comic splash: ink at the top fading into halftone and paper.
struct OnboardingScreen: View {
    let onDone: () -> Void
    @State private var languages = Set(["pt-br"])
    @State private var webtoon = false
    @State private var starting = false

    private static let ink = Color(red: 0x16 / 255, green: 0x16 / 255, blue: 0x1A / 255)
    private static let paper = Color(red: 0xF6 / 255, green: 0xF1 / 255, blue: 0xE7 / 255)
    private static let note = Color(red: 1, green: 0xF1 / 255, blue: 0xA8 / 255)

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 20) {
                    Text(L("onboarding_reading_question"))
                        .font(ComicFont.bangers(40, relativeTo: .largeTitle))
                        .foregroundStyle(Self.paper)
                        .shadow(color: Palette.primary, radius: 0, x: 2, y: 2)
                        .multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                    Text(L("onboarding_reading_hint"))
                        .font(.subheadline)
                        .foregroundStyle(Self.paper)
                        .multilineTextAlignment(.center)
                    VStack(spacing: 16) {
                        card(L("onboarding_paged"), tag: L("onboarding_paged_tag"), selected: !webtoon, tilt: -2) { webtoon = false }
                        card(L("onboarding_webtoon"), tag: L("onboarding_webtoon_tag"), selected: webtoon, tilt: 1.5) { webtoon = true }
                    }
                    Text(L("onboarding_select_language"))
                        .font(ComicFont.bangers(22, relativeTo: .title3))
                        .foregroundStyle(Palette.onSurface)
                    FlowLayout(spacing: 8) {
                        ForEach(ContentLanguageKt.CONTENT_LANGUAGES, id: \.label) { language in
                            let codes = Set(language.codes)
                            let on = !languages.isDisjoint(with: codes)
                            Button {
                                let next = on ? languages.subtracting(codes) : languages.union(codes)
                                if !next.isEmpty { languages = next }
                            } label: {
                                Label(language.label, systemImage: on ? "checkmark" : "plus")
                            }
                            .buttonStyle(.bordered)
                            .tint(on ? Palette.primary : Palette.onSurfaceVariant)
                            .accessibilityAddTraits(on ? .isSelected : [])
                        }
                    }
                    Text(L("onboarding_no_content"))
                        .font(.footnote)
                        .foregroundStyle(Self.ink)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 12).padding(.vertical, 8)
                        .background(Self.note)
                        .overlay(Rectangle().stroke(Self.ink, lineWidth: 2))
                        .rotationEffect(.degrees(-1))
                }
                .padding(.horizontal, 24)
                .padding(.top, 40)
                .padding(.bottom, 16)
                .frame(maxWidth: 560)
                .frame(maxWidth: .infinity)
            }
            Button {
                starting = true
                launch {
                    try await Core.graph.finishOnboarding(languages: Array(languages), webtoon: webtoon)
                    onDone()
                }
            } label: {
                Text(L("onboarding_start")).font(.headline).frame(maxWidth: .infinity).frame(height: 44)
            }
            .buttonStyle(.borderedProminent)
            .disabled(starting)
            .accessibilityIdentifier("onboarding.start")
            .frame(maxWidth: 560)
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
        .background {
            ZStack(alignment: .top) {
                Palette.background
                LinearGradient(stops: [.init(color: Self.ink, location: 0),
                                       .init(color: Self.ink.opacity(0.6), location: 0.3),
                                       .init(color: Palette.background, location: 0.48)],
                               startPoint: .top, endPoint: .bottom)
                GeometryReader { proxy in
                    Halftone(color: Self.ink.opacity(0.25), step: 6, radius: 1.4)
                        .frame(height: proxy.size.height * 0.42)
                        .mask(LinearGradient(colors: [.black, .clear], startPoint: .top, endPoint: .bottom))
                }
            }
            .ignoresSafeArea()
        }
        .onAppear { languages = Self.saved() }
    }

    /// One reading mode as a hand-drawn card with its tag; a radio button for assistive technology.
    private func card(_ title: String, tag: String, selected: Bool, tilt: Double, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(ComicFont.bangers(24, relativeTo: .title2))
                .foregroundStyle(Self.ink)
                .multilineTextAlignment(.center)
                .frame(width: 220)
                .padding(.vertical, 20).padding(.horizontal, 16)
                .background(Color(red: 1, green: 0xFD / 255, blue: 0xF7 / 255))
                .overlay(Rectangle().stroke(selected ? Palette.primary : Self.ink, lineWidth: selected ? 4 : 2))
                .background(Self.ink.offset(x: 5, y: 5))
                .overlay(alignment: .topTrailing) {
                    Text(tag)
                        .font(ComicFont.bangers(15, relativeTo: .caption))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 8).padding(.vertical, 2)
                        .background(selected ? Palette.primary : Self.ink)
                        .rotationEffect(.degrees(6))
                        .offset(x: 10, y: -10)
                }
        }
        .buttonStyle(.plain)
        .rotationEffect(.degrees(tilt))
        .accessibilityAddTraits(selected ? [.isSelected] : [])
        .accessibilityLabel("\(title), \(tag)")
    }

    private static func saved() -> Set<String> {
        let value = Core.graph.preferences().contentLanguages.value
        let set = (value as? Set<String>) ?? Set((value as? NSSet)?.compactMap { $0 as? String } ?? [])
        return set.isEmpty ? ["pt-br"] : set
    }
}

#Preview("OnboardingScreen · Light") { OnboardingScreen {}.previewEnvironment().preferredColorScheme(.light) }
#Preview("OnboardingScreen · Dark") { OnboardingScreen {}.previewEnvironment().preferredColorScheme(.dark) }
