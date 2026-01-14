rootProject.name = "rate-limiter"

include("core")
include("strategies:fixed-window")
include("strategies:sliding-window")
include("strategies:token-bucket")
include("app")
