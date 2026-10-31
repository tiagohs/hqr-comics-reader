import Shared
import SwiftUI

struct RootView: View {
    var body: some View {
        Text(L("app_name"))
    }
}

#Preview("RootView · Light") { RootView().preferredColorScheme(.light) }
#Preview("RootView · Dark") { RootView().preferredColorScheme(.dark) }
