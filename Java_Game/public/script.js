const API = "/api";
let sessionId = null;

const setupEl = document.getElementById("setup");
const gameEl = document.getElementById("game");
const rangeText = document.getElementById("rangeText");
const triesText = document.getElementById("triesText");
const messageEl = document.getElementById("message");
const historyBox = document.getElementById("historyBox");
const guessInput = document.getElementById("guessInput");
const newGameBtn = document.getElementById("newGameBtn");

document.getElementById("startBtn").addEventListener("click", startGame);
document.getElementById("guessBtn").addEventListener("click", submitGuess);
guessInput.addEventListener("keydown", (e) => { if (e.key === "Enter") submitGuess(); });
newGameBtn.addEventListener("click", resetToSetup);

async function startGame() {
  const name = document.getElementById("name").value.trim() || "Player";
  const difficulty = document.getElementById("difficulty").value;

  const res = await fetch(`${API}/new`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, difficulty })
  });
  const data = await res.json();

  sessionId = data.sessionId;
  rangeText.textContent = `Guess a number between ${data.min} and ${data.max}.`;
  triesText.textContent = `Tries: 0 / ${data.maxTries}`;
  messageEl.textContent = "";
  historyBox.textContent = "";
  guessInput.value = "";
  newGameBtn.classList.add("hidden");

  setupEl.classList.add("hidden");
  gameEl.classList.remove("hidden");
  guessInput.focus();
}

async function submitGuess() {
  const guess = parseInt(guessInput.value, 10);
  if (Number.isNaN(guess)) {
    messageEl.textContent = "Please enter a valid number.";
    return;
  }

  const res = await fetch(`${API}/guess`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ sessionId, guess })
  });
  const data = await res.json();

  if (data.error) {
    messageEl.textContent = data.error;
    return;
  }

  triesText.textContent = `Tries: ${data.tries} / ${data.maxTries}`;
  messageEl.textContent = data.message;
  historyBox.textContent = `Guesses so far: ${data.history.join(", ")}`;
  guessInput.value = "";

  if (data.over) {
    if (data.won) {
      messageEl.textContent += ` 🎉 You scored ${data.score} points!`;
    } else {
      messageEl.textContent += ` Out of tries — the number was ${data.answer}.`;
    }
    newGameBtn.classList.remove("hidden");
    loadLeaderboard();
  }
}

function resetToSetup() {
  gameEl.classList.add("hidden");
  setupEl.classList.remove("hidden");
}

async function loadLeaderboard() {
  const res = await fetch(`${API}/leaderboard`);
  const scores = await res.json();
  const tbody = document.querySelector("#leaderboard tbody");
  tbody.innerHTML = "";
  scores.forEach((s, i) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td>${i + 1}</td><td>${s.name}</td><td>${s.score}</td><td>${s.difficulty}</td>`;
    tbody.appendChild(tr);
  });
}

loadLeaderboard();