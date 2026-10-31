import XCTest

/// The app opens on the library with an empty, in-memory core (no network needed).
final class LaunchUITests: XCTestCase {
    func test_SCR_LIB_001_app_opens() {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTests"]
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10))
    }

    func test_RF81_onboarding_comes_first_and_leads_to_the_library() {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTests"]
        app.launch()
        let start = app.buttons["onboarding.start"]
        XCTAssertTrue(start.waitForExistence(timeout: 10))
        start.tap()
        XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout: 10) || app.collectionViews.firstMatch.waitForExistence(timeout: 1))
        XCTAssertFalse(start.exists)
    }
}
