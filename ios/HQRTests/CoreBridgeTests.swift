import Shared
import XCTest
@testable import HQR

/// The Kotlin core as the app sees it through the Shared framework.
@MainActor
final class CoreBridgeTests: XCTestCase {
    override func setUp() {
        Core.start()
    }

    func test_FEAT060_library_view_model_starts_loading() {
        let model = ScreenModel(Core.graph.libraryViewModel()) { Screen<LibraryViewModel.State>(viewModel: $0, flow: $0.state) }
        XCTAssertNotNil(model.state)
    }

    func test_RF75_backup_bytes_cross_the_bridge_unchanged() {
        let bytes = Core.graph.bytes(data: Data([1, 2, 3, 250]))
        XCTAssertEqual(Core.graph.data(bytes: bytes), Data([1, 2, 3, 250]))
    }
}
