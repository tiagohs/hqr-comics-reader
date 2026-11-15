import Shared
import SwiftUI

/// SCR-SET-003: version, what HQR is (and is not), credits, privacy and the source code (RF-78, RF-79).
struct AboutScreen: View {
    private let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0.0"

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 8) {
                    Text(L("app_name")).font(.largeTitle.weight(.heavy))
                    Text(L("about_version_ios", version)).font(.subheadline).foregroundStyle(Palette.onSurfaceVariant)
                    Text(L("about_no_content")).font(.subheadline)
                }
                .padding(.vertical, 4)
            }
            Section(L("about_credits")) {
                Text(L("about_credit_mangadex"))
                Text(L("about_credit_hqnow"))
                Text(L("about_based_on"))
            }
            Section(L("about_privacy")) {
                Text(L("about_privacy_text_ios")).font(.subheadline)
                Link(L("about_privacy_link"), destination: URL(string: "https://tiagohs.github.io/hqr-comics-reader/politicy/")!)
            }
            Section {
                Link(L("about_source_code"), destination: URL(string: "https://github.com/tiagohs/hqr-comics-reader")!)
            }
        }
        .navigationTitle(L("settings_about"))
    }
}

#Preview("AboutScreen · Light") { AboutScreen().previewScreen().preferredColorScheme(.light) }
#Preview("AboutScreen · Dark") { AboutScreen().previewScreen().preferredColorScheme(.dark) }
