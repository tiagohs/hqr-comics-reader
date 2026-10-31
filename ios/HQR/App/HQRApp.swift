import Shared
import SwiftUI

@main
struct HQRApp: App {
    init() {
        Core.start()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
