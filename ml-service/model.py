import numpy as np
from sklearn.linear_model import LinearRegression

MIN_MONTHS_REQUIRED = 3


def forecast_next_month(history):
    """
    history: list of {"month": "2026-01", "amount": 4200.0}, sorted oldest -> newest

    Returns: {"predictedAmount": float, "confidenceNote": str}
    """
    if len(history) < MIN_MONTHS_REQUIRED:
        # Cold start: not enough history for a regression to be meaningful
        avg = (
            sum(entry["amount"] for entry in history) / len(history)
            if history
            else 0.0
        )
        return {
            "predictedAmount": round(avg, 2),
            "confidenceNote": (
                f"Only {len(history)} month(s) of data available; showing a simple "
                f"average instead of a trend-based forecast. Need at least "
                f"{MIN_MONTHS_REQUIRED} months for a regression estimate."
            ),
        }

    amounts = np.array([entry["amount"] for entry in history])
    months_idx = np.arange(len(history)).reshape(-1, 1)

    model = LinearRegression()
    model.fit(months_idx, amounts)

    next_month_idx = np.array([[len(history)]])
    prediction = model.predict(next_month_idx)[0]
    prediction = max(prediction, 0.0)  # spend can't be negative

    return {
        "predictedAmount": round(float(prediction), 2),
        "confidenceNote": (
            f"Linear trend estimated from {len(history)} months of data."
        ),
    }
