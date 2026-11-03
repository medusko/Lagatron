// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "LagatronMac",
    platforms: [
        .macOS(.v13)
    ],
    products: [
        .executable(name: "LagatronMac", targets: ["LagatronMac"])
    ],
    targets: [
        .executableTarget(
            name: "LagatronMac",
            path: "Sources/LagatronMac",
            resources: [
                .process("Resources")
            ]
        )
    ]
)
