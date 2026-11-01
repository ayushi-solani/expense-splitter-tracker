from flask import Flask, request, jsonify
from model import forecast_next_month, MIN_MONTHS_REQUIRED

app = Flask(__name__)


@app.route("/forecast/overall", methods=["POST"])
def forecast_overall():
    data = request.get_json(force=True)
    history = data.get("history", [])
    result = forecast_next_month(history)
    return jsonify(result)


@app.route("/forecast/category/<category>", methods=["POST"])
def forecast_category(category):
    data = request.get_json(force=True)
    history = data.get("history", [])
    result = forecast_next_month(history)
    result["category"] = category
    return jsonify(result)


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "min_months_required": MIN_MONTHS_REQUIRED})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
