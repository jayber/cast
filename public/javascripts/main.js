var ws;
var endScore = 0;
var maxSpellSize = 0;

$(document).ready(function () {

    setUpSocket();

    $("#buttonA").click(function (event) {
        addToSpell('A');
    });
    $("#buttonB").click(function (event) {
        addToSpell('B');
    });
    $("#buttonC").click(function (event) {
        addToSpell('C');
    });
    $("#buttonD").click(function (event) {
        addToSpell('D');
    });

    $("#buttonCast").click(function (event) {
        castSpell();
    });
});


function addToSpell(symbol) {
    var spell = $("#castingArea").text();
    if (spell.length < maxSpellSize) {
        $("#castingArea").text(spell + symbol);
    } else {
        displayMessage("Spells can only be " + maxSpellSize + " symbols long!");
    }
}

function clearCastingArea() {
    $("#castingArea").html("&nbsp");
}

function castSpell() {
    var spell = $("#castingArea").text().trim();
    if (canCast(spell)) {
        sendCast(spell);
        clearCastingArea();
    } else {
        displayMessage("Not enough energy to cast spell!");
    }
}

function canCast(spell) {
    var playerName = playerNames['player'];
    return spell.length < gameState[playerName].score
}

function sendCast(spell) {
    ws.send(spell);
}

function requestOpponents() {
    ws.send("sendOpponents");
}

function ready() {
    ws.send("startGame");
}

function updateOpponentList(opponent) {
    var target = $("#opponents");
    var content = target.html();
    target.html(content + "<br>" +
        "<a href='/playGame" + window.location.search + "&opponent=" + opponent + "'>" + opponent + "</a>")
}

function setUpSocket() {
    ws = new WebSocket("ws://" + window.location.host + "/cast");

    ws.onmessage = messageHandler;

    ws.onopen = function (evt) {
        console.log("Open...");
    };

    ws.onerror = function (evt) {
        console.log("error: " + evt.error)
    }
}

function calcScoreBarWidth(score) {

    var width = $("#opponentScore")[0].width;
    var widthUnit = width / endScore;

    if (score > endScore) {
        score = endScore;
    }

    return score * widthUnit;
}

function spellHtml(spells, reverse) {
    var template1 = "<div class='spell'>";
    var template2 = "</div>";
    var result = "";
    for (var i = 0; i < spells.length; i++) {
        if (reverse) {
            result = template1 + (spells[i] == "" ? "&nbsp;" : spells[i]) + template2 + result;
        } else {
            result = result + template1 + (spells[i] == "" ? "&nbsp;" : spells[i]) + template2;
        }
    }
    return result;
}

function displayMessage(msg) {
    $("#messages").html("<div>" + msg + "</div>" + $("#messages").html());
}

function messageHandler(evt) {
    console.log(evt.data);
    var received_msg = JSON.parse(evt.data);
    if (received_msg.hasOwnProperty('opponent')) {
        var opponent = received_msg['opponent'];
        updateOpponentList(opponent)
    }
    if (received_msg.hasOwnProperty('message')) {
        $("button").removeAttr("disabled");
        var msg = received_msg['message'];
        displayMessage(msg);
    }
    if (received_msg.hasOwnProperty('playing')) {
        var state = received_msg['playing'];
        updatePlayingState(state);
    }
    if (received_msg.hasOwnProperty('winner')) {
        $("button").attr("disabled", true);
        var winner = received_msg['winner'];
        $("#winner").html(winner + " wins!");
    }
    if (received_msg.hasOwnProperty('strike')) {
        var name = received_msg['strike'];
        var role = playerRoles[name];
        if (role=="player") {
            clearCastingArea();
            flashScore($("#playerScore"));
        } else {
            flashScore($("#opponentScore"));
        }
    }
}

function flashScore(target) {

    makeRed(target);

    setTimeout(function() {
        makeBlack(target);
        setTimeout(function() {
            makeRed(target);
            setTimeout(function() {
                makeBlack(target);
            }, 75);
        }, 75)
    }, 75);
}

function makeRed(target) {
    target.css("border-color","#ff0000");
    target.css("background-color","#ff0000");
}

function makeBlack(target) {
    target.css("border-color","#000000");
    target.css("background-color","#ffffff");
}

function updatePlayingState(state) {
    gameState = state;
    var playerName = playerNames['player'];
    var opponentName = playerNames['opponent'];
    $("#player").html(spellHtml(state[playerName]['spells'], true));
    displayScore($("#playerScore"),calcScoreBarWidth(state[playerName]['score']));
    $("#opponent").html(spellHtml(state[opponentName]['spells']));
    displayScore($("#opponentScore"),calcScoreBarWidth(state[opponentName]['score']));
}

function displayScore(elements, width) {
    var element = elements[0];
    var ctx = element.getContext("2d");
    ctx.fillStyle = "#ff6633";
    ctx.fillRect(0,0,width,element.height);
}