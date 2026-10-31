import XCTest

/// The app opens on the library with an empty, in-memory core (no network needed).
final class LaunchUITests: XCTestCase {
    func test_SCR_LIB_001_app_opens() {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTests"]
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10))
    }
}
