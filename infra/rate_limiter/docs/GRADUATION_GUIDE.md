# Graduation Guide: Moving from Simulation to Operational

This guide explains the process of "graduating" a rate-limiting policy from the **Explorer Profile (Simulation Mode)** to the **Governed Profile (Operational Mode)**.

## 1. The Core Philosophy

The Policy-Driven Rate Limiter SDK uses a "Same Same But Different" model. The engine is identical, but the **Capability Profile** determines what is allowed.

- **Simulation Mode**: Permissive. Allows "unsafe" configurations (e.g., extremely short windows, high burst capacity). Emits warnings but executes anyway.
- **Operational Mode**: Strict. Enforces **Safe Operating Envelopes**. Rejects unsafe configurations with an `InstabilityRiskException`.

## 2. Step-by-Step Graduation

### Step 1: Stability Analysis (Simulation)
Run your strategy in the **Rate Limiter Lab** using Simulation Mode.
- Observe the **Oscillation Visualization** (Throughput Chart).
- Look for "Jagged" throughput. If your throughput oscillates wildly, your window size might be too small or your limit too tight.
- **Goal**: Achieve a "Smooth" throughput curve.

### Step 2: Policy Hardening
Once you have found stable parameters, define your **Control Law** (Policy DSL).

```json
{
  "policies": [
    {
      "name": "Production_API_Tier",
      "then": {
        "use": "token_bucket",
        "params": {
          "capacity": "100",
          "refillRate": "10"
        }
      }
    }
  ]
}
```

### Step 3: Switch Capability Profile
Update your `DecisionClient` configuration to use the **Operational Validator**.

```kotlin
val client = DecisionClient.builder()
    .withPolicyJson(hardenedPolicy)
    .withOperationalMode() // Enforces Safe Operating Envelope
    .build()
```

### Step 4: Shadow Deployment
Before full enforcement, run in **Shadow Mode**. This allows you to see what decisions the engine *would* have made without actually blocking traffic.

```kotlin
val client = DecisionClient.builder()
    .withPolicyJson(hardenedPolicy)
    .withOperationalMode()
    .withShadowMode(true) // Observe without enforcing
    .build()
```

Monitor your metrics. If the "Would Block" rate matches your expectations, disable Shadow Mode.

## 3. Safe Operating Envelopes

The following constraints are enforced in **Operational Mode**:

| Strategy | Constraint | Reason |
| :--- | :--- | :--- |
| **Fixed Window** | `windowSizeMs >= 1000` | Avoids high-frequency state churn. |
| **Token Bucket** | `refillRate <= capacity * 2` | Prevents "infinite burst" configurations. |
| **Sliding Window** | `windowSizeMs >= 5000` | Memory safety (Sliding Log can be expensive). |

## 4. Troubleshooting Graduation

### `InstabilityRiskException`
If you receive this exception during initialization, your policy parameters fall outside the **Safe Operating Envelope**.
- **Fix**: Increase `windowSizeMs` or reduce `capacity` to meet the safety requirements of the Operational Profile.

### High Latency
If the `Execution Trace` shows > 5ms decision time:
- Switch from `sliding_window_log` to `token_bucket`.
- Ensure you are using the `InMemoryStateStore` or a fast Redis connection.
