// Football Season Simulator - Frontend Application
let stompClient = null;
let currentLeague = 'premier-league';
const leagueData = {};
let pollingInterval = null;
let wsConnected = false;

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    initializeLeagueTabs();
    connectWebSocket();
    loadLeagueData(currentLeague);
    startPolling();
});

// Polling fallback - refresh every 2 seconds for smooth updates
function startPolling() {
    if (pollingInterval) clearInterval(pollingInterval);
    pollingInterval = setInterval(() => {
        loadLeagueData(currentLeague);
    }, 2000);
}

// Initialize league tab switching
function initializeLeagueTabs() {
    document.querySelectorAll('.league-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            document.querySelectorAll('.league-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            currentLeague = this.dataset.leagueId;
            loadLeagueData(currentLeague);
        });
    });
}

// WebSocket connection
function connectWebSocket() {
    try {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function(frame) {
            updateConnectionStatus(true);
            ['premier-league', 'la-liga', 'serie-a', 'bundesliga', 'ligue-1'].forEach(league => {
                stompClient.subscribe(`/topic/league/${league}/events`, function(message) {
                    handleMatchEvent(league, JSON.parse(message.body));
                });
                stompClient.subscribe(`/topic/league/${league}/status`, function(message) {
                    handleStatusUpdate(league, JSON.parse(message.body));
                });
            });
        }, function(error) {
            updateConnectionStatus(false);
            setTimeout(connectWebSocket, 5000);
        });
    } catch (e) {
        updateConnectionStatus(false);
    }
}

function updateConnectionStatus(connected) {
    wsConnected = connected;
    const status = document.getElementById('connection-status');
    if (connected) {
        status.textContent = 'LIVE';
        status.className = 'status-badge';
    } else {
        status.textContent = 'POLLING';
        status.className = 'status-badge polling';
    }
}

// Load league data from API
async function loadLeagueData(leagueId) {
    try {
        const [fixtureRes, standingsRes, statusRes, nextFixtureRes] = await Promise.all([
            fetch(`/api/leagues/${leagueId}/fixture`),
            fetch(`/api/leagues/${leagueId}/standings`),
            fetch(`/api/leagues/${leagueId}/status`),
            fetch(`/api/leagues/${leagueId}/next-fixture`)
        ]);

        const fixture = await fixtureRes.json();
        const standings = await standingsRes.json();
        const status = await statusRes.json();
        let nextFixture = null;
        if (nextFixtureRes.ok) {
            nextFixture = await nextFixtureRes.json();
        }

        leagueData[leagueId] = { fixture, standings, status, nextFixture };

        renderFixtures(fixture, status);
        renderNextFixtures(nextFixture, status);
        renderStandings(standings);
        updateOverallMinute(fixture);
    } catch (error) {
        console.error('Error loading league data:', error);
    }
}

// Render current fixtures
function renderFixtures(fixture, status) {
    const container = document.getElementById('fixturesContainer');
    const section = document.getElementById('currentMatchweek');

    if (!fixture || !fixture.matches) {
        container.innerHTML = '<div class="loading-state"><p>Waiting for matches...</p></div>';
        return;
    }

    // Update matchweek header
    section.querySelector('.matchweek-number').textContent = fixture.matchweek;
    section.querySelector('.season-badge').textContent = `Season ${status?.season || fixture.season}`;

    // Check if we need to rebuild the DOM or just update scores
    const existingRows = container.querySelectorAll('.match-row');
    const needsRebuild = existingRows.length !== fixture.matches.length ||
        (existingRows.length > 0 && existingRows[0].dataset.matchId !== fixture.matches[0].id);

    if (needsRebuild) {
        // Full rebuild - only when matchweek changes
        let html = '';
        fixture.matches.forEach(match => {
            const isLive = match.phase === 'FIRST_HALF' || match.phase === 'SECOND_HALF' || match.phase === 'HALF_TIME';
            const isFinished = match.phase === 'FULL_TIME';
            const phaseText = getPhaseText(match.phase);
            const minuteText = isFinished ? 'FT' : (isLive ? match.timeDisplay : '--');
            const homeWinner = isFinished && match.homeScore > match.awayScore;
            const awayWinner = isFinished && match.awayScore > match.homeScore;

            html += `
                <div class="match-row ${isLive ? 'live' : ''} ${isFinished ? 'finished' : ''}"
                     data-match-id="${match.id}" onclick="showMatchDetails('${match.id}')" style="cursor: pointer;">
                    <div class="match-status-col">
                        <span class="match-minute ${isLive ? 'live' : ''}">${minuteText}</span>
                        ${isLive ? `<span class="match-phase">${phaseText}</span>` : ''}
                    </div>
                    <div class="match-content">
                        <div class="team-home ${homeWinner ? 'winner' : ''}">
                            <span class="team-name">${match.homeTeamName}</span>
                            <img src="${match.homeTeamBadge || '/img/default-badge.png'}" class="team-badge"
                                 alt="" onerror="this.style.visibility='hidden'">
                        </div>
                        <div class="match-score-inline ${isLive ? 'live' : ''}">
                            <span class="score-home ${homeWinner ? 'winner' : ''}">${match.homeScore}</span>
                            <span class="score-separator">-</span>
                            <span class="score-away ${awayWinner ? 'winner' : ''}">${match.awayScore}</span>
                        </div>
                        <div class="team-away ${awayWinner ? 'winner' : ''}">
                            <img src="${match.awayTeamBadge || '/img/default-badge.png'}" class="team-badge"
                                 alt="" onerror="this.style.visibility='hidden'">
                            <span class="team-name">${match.awayTeamName}</span>
                        </div>
                    </div>
                </div>`;
        });
        container.innerHTML = html || '<div class="loading-state"><p>No matches</p></div>';
    } else {
        // Incremental update - just update scores and status (no blinking)
        fixture.matches.forEach(match => {
            const row = container.querySelector(`[data-match-id="${match.id}"]`);
            if (row) {
                const isLive = match.phase === 'FIRST_HALF' || match.phase === 'SECOND_HALF' || match.phase === 'HALF_TIME';
                const isFinished = match.phase === 'FULL_TIME';
                const phaseText = getPhaseText(match.phase);
                const minuteText = isFinished ? 'FT' : (isLive ? match.timeDisplay : '--');
                const homeWinner = isFinished && match.homeScore > match.awayScore;
                const awayWinner = isFinished && match.awayScore > match.homeScore;

                // Update classes
                row.className = `match-row ${isLive ? 'live' : ''} ${isFinished ? 'finished' : ''}`;

                // Update minute display
                const minuteEl = row.querySelector('.match-minute');
                if (minuteEl) {
                    minuteEl.textContent = minuteText;
                    minuteEl.className = `match-minute ${isLive ? 'live' : ''}`;
                }

                // Update scores
                const homeScoreEl = row.querySelector('.score-home');
                const awayScoreEl = row.querySelector('.score-away');
                if (homeScoreEl) {
                    homeScoreEl.textContent = match.homeScore;
                    homeScoreEl.className = `score-home ${homeWinner ? 'winner' : ''}`;
                }
                if (awayScoreEl) {
                    awayScoreEl.textContent = match.awayScore;
                    awayScoreEl.className = `score-away ${awayWinner ? 'winner' : ''}`;
                }

                // Update score container live class
                const scoreContainer = row.querySelector('.match-score-inline');
                if (scoreContainer) {
                    scoreContainer.className = `match-score-inline ${isLive ? 'live' : ''}`;
                }

                // Update team winner classes
                const teamHome = row.querySelector('.team-home');
                const teamAway = row.querySelector('.team-away');
                if (teamHome) teamHome.className = `team-home ${homeWinner ? 'winner' : ''}`;
                if (teamAway) teamAway.className = `team-away ${awayWinner ? 'winner' : ''}`;
            }
        });
    }
}

// Helper to get phase text
function getPhaseText(phase) {
    const phases = {
        'FIRST_HALF': '1st Half',
        'HALF_TIME': 'HT',
        'SECOND_HALF': '2nd Half',
        'FULL_TIME': 'FT',
        'NOT_STARTED': ''
    };
    return phases[phase] || '';
}

// Track current ticker matchweek to avoid unnecessary rebuilds
let currentTickerMatchweek = null;

// Render next matchweek fixtures in ticker footer
function renderNextFixtures(fixture, status) {
    const ticker = document.getElementById('nextMatchweekTicker');
    const tickerContent = document.getElementById('tickerContent');
    const matchweekNumber = document.querySelector('.next-matchweek-number');

    if (!fixture || !fixture.matches || fixture.matches.length === 0) {
        ticker.classList.add('hidden');
        currentTickerMatchweek = null;
        return;
    }

    ticker.classList.remove('hidden');
    matchweekNumber.textContent = fixture.matchweek;

    // Only rebuild ticker if matchweek changed (prevents blinking)
    if (currentTickerMatchweek === fixture.matchweek) {
        return;
    }
    currentTickerMatchweek = fixture.matchweek;

    // Build ticker HTML - use full team names
    let matchesHtml = fixture.matches.map(match => `
        <div class="ticker-match">
            <div class="ticker-team">
                <img src="${match.homeTeamBadge || '/img/default-badge.png'}"
                     alt="" onerror="this.style.visibility='hidden'">
                <span>${match.homeTeamName}</span>
            </div>
            <span class="ticker-vs">vs</span>
            <div class="ticker-team">
                <span>${match.awayTeamName}</span>
                <img src="${match.awayTeamBadge || '/img/default-badge.png'}"
                     alt="" onerror="this.style.visibility='hidden'">
            </div>
        </div>
    `).join('');

    // Duplicate content for seamless infinite scroll
    tickerContent.innerHTML = matchesHtml + matchesHtml;
}

// Track standings state to minimize rebuilds
let lastStandingsHash = null;

// Render standings table
function renderStandings(standings) {
    const container = document.getElementById('standingsContainer');
    if (!standings || standings.length === 0) {
        container.innerHTML = '<div class="loading-state"><p>No standings available</p></div>';
        lastStandingsHash = null;
        return;
    }

    // Create a simple hash to detect changes
    const standingsHash = standings.map(t => `${t.teamId}:${t.played}:${t.won}:${t.drawn}:${t.lost}`).join('|');
    if (standingsHash === lastStandingsHash) {
        return; // No changes, skip rebuild
    }
    lastStandingsHash = standingsHash;

    let html = `
        <table class="standings-table">
            <thead>
                <tr>
                    <th>#</th>
                    <th>Team</th>
                    <th>P</th>
                    <th>W</th>
                    <th>D</th>
                    <th>L</th>
                    <th>GD</th>
                    <th>Pts</th>
                </tr>
            </thead>
            <tbody>`;

    standings.forEach((team, index) => {
        const position = index + 1;
        const gd = team.goalsFor - team.goalsAgainst;
        const pts = (team.won * 3) + team.drawn;
        let posClass = '';
        if (position <= 4) posClass = 'ucl';
        else if (position <= 6) posClass = 'uel';
        else if (position >= standings.length - 2) posClass = 'relegation';

        html += `
            <tr class="${posClass}" data-team-id="${team.teamId}">
                <td class="pos">${position}</td>
                <td>
                    <div class="team-cell">
                        <img src="${team.teamBadgeUrl || '/img/default-badge.png'}"
                             alt="" onerror="this.style.visibility='hidden'">
                        <span>${team.teamName}</span>
                    </div>
                </td>
                <td>${team.played}</td>
                <td>${team.won}</td>
                <td>${team.drawn}</td>
                <td>${team.lost}</td>
                <td>${gd > 0 ? '+' : ''}${gd}</td>
                <td class="pts">${pts}</td>
            </tr>`;
    });

    html += '</tbody></table>';
    container.innerHTML = html;
}

// Update overall minute display
function updateOverallMinute(fixture) {
    const minuteDisplay = document.getElementById('overallMinute');
    if (!fixture || !fixture.matches || fixture.matches.length === 0) {
        minuteDisplay.textContent = "--'";
        return;
    }

    // Find the first live match to get the current minute
    const liveMatch = fixture.matches.find(m =>
        m.phase === 'FIRST_HALF' || m.phase === 'SECOND_HALF' || m.phase === 'HALF_TIME'
    );

    if (liveMatch && liveMatch.timeDisplay) {
        minuteDisplay.textContent = liveMatch.timeDisplay;
    } else if (fixture.matches.every(m => m.phase === 'FULL_TIME')) {
        minuteDisplay.textContent = "FT";
    } else {
        minuteDisplay.textContent = "--'";
    }
}

// Handle real-time match events
function handleMatchEvent(leagueId, event) {
    if (leagueId !== currentLeague) return;

    // Update the match row
    const matchRow = document.querySelector(`[data-match-id="${event.matchId}"]`);
    if (matchRow) {
        const scoreSpans = matchRow.querySelectorAll('.match-score span');
        const statusDisplay = matchRow.querySelector('.match-status');

        if (scoreSpans.length >= 3) {
            scoreSpans[0].textContent = event.homeScore;
            scoreSpans[2].textContent = event.awayScore;
        }
        if (statusDisplay) {
            statusDisplay.textContent = `${event.minute}'`;
        }

        // Update overall minute
        const minuteDisplay = document.getElementById('overallMinute');
        if (minuteDisplay) {
            minuteDisplay.textContent = `${event.minute}'`;
        }

        // Add visual feedback for goals
        if (event.type === 'GOAL' || event.type === 'PENALTY_SCORED') {
            matchRow.classList.add('goal-scored');
            setTimeout(() => matchRow.classList.remove('goal-scored'), 2000);
        }
    }
}

// Handle status updates (matchweek changes, season changes)
function handleStatusUpdate(leagueId, status) {
    // Reload fixtures when matchweek changes
    if (leagueId === currentLeague) {
        loadLeagueData(leagueId);
    }
}

// Show match details modal
async function showMatchDetails(matchId) {
    const modal = new bootstrap.Modal(document.getElementById('matchModal'));
    const modalBody = document.getElementById('matchModalBody');

    modalBody.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border" role="status"></div>
            <p class="mt-2">Loading match details...</p>
        </div>`;
    modal.show();

    try {
        const [matchRes, eventsRes] = await Promise.all([
            fetch(`/api/matches/${matchId}`),
            fetch(`/api/matches/${matchId}/events`)
        ]);

        const match = await matchRes.json();
        const events = await eventsRes.json();

        renderMatchModal(match, events);
    } catch (error) {
        modalBody.innerHTML = '<p class="text-center text-danger">Error loading match details</p>';
    }
}

// Render match modal content
function renderMatchModal(match, events) {
    const modalBody = document.getElementById('matchModalBody');
    const modalTitle = document.getElementById('matchModalLabel');

    modalTitle.textContent = `${match.homeTeam.name} vs ${match.awayTeam.name}`;

    const isFinished = match.phase === 'FULL_TIME';
    const eventsHtml = renderEventsTab(match, events);
    const statsHtml = renderStatsTab(match, events);
    const predictionsHtml = renderPredictionsTab(match, events);
    const commentaryHtml = renderCommentaryTab(match, events);

    modalBody.innerHTML = `
        <div class="text-center mb-4">
            <div class="d-flex align-items-center justify-content-center">
                <div class="text-center me-4">
                    <img src="${match.homeTeam.badgeUrl || '/img/default-badge.png'}" class="mb-2"
                         style="height: 64px;" onerror="this.src='/img/default-badge.png'">
                    <h5>${match.homeTeam.name}</h5>
                </div>
                <div class="score-display mx-4" style="font-size: 2.5rem;">
                    ${match.homeScore} - ${match.awayScore}
                </div>
                <div class="text-center ms-4">
                    <img src="${match.awayTeam.badgeUrl || '/img/default-badge.png'}" class="mb-2"
                         style="height: 64px;" onerror="this.src='/img/default-badge.png'">
                    <h5>${match.awayTeam.name}</h5>
                </div>
            </div>
            <p class="text-muted mt-2">${isFinished ? 'Full Time' : match.timeDisplay}</p>
        </div>

        <ul class="nav nav-tabs modal-tabs" role="tablist">
            <li class="nav-item" role="presentation">
                <button class="nav-link active" data-bs-toggle="tab" data-bs-target="#eventsTab" type="button">
                    <i class="bi bi-list-ul me-1"></i>Events
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" data-bs-toggle="tab" data-bs-target="#commentaryTab" type="button">
                    <i class="bi bi-chat-left-text me-1"></i>Commentary
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" data-bs-toggle="tab" data-bs-target="#statsTab" type="button">
                    <i class="bi bi-bar-chart me-1"></i>Statistics
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" data-bs-toggle="tab" data-bs-target="#predictionsTab" type="button">
                    <i class="bi bi-graph-up me-1"></i>Predictions
                </button>
            </li>
        </ul>

        <div class="tab-content mt-3">
            <div class="tab-pane fade show active" id="eventsTab">${eventsHtml}</div>
            <div class="tab-pane fade" id="commentaryTab">${commentaryHtml}</div>
            <div class="tab-pane fade" id="statsTab">${statsHtml}</div>
            <div class="tab-pane fade" id="predictionsTab">${predictionsHtml}</div>
        </div>`;
}

// Render Events Tab
function renderEventsTab(match, events) {
    let eventsHtml = '';
    const significantEvents = events.filter(e =>
        ['GOAL', 'PENALTY_SCORED', 'PENALTY_MISSED', 'OWN_GOAL', 'YELLOW_CARD', 'RED_CARD', 'VAR_CHECK'].includes(e.type.name || e.type)
    );

    significantEvents.forEach(event => {
        const eventType = event.type.name || event.type;
        const eventClass = getEventClass(eventType);
        const icon = getEventIcon(eventType);
        const isHome = event.teamId === match.homeTeam.id;
        const teamAbbr = isHome ? match.homeTeam.shortName : match.awayTeam.shortName;
        const eventTypeClass = getEventTypeClass(eventType);
        eventsHtml += `
            <div class="event-item ${eventClass}">
                <span class="badge bg-secondary me-2">${event.displayTime || event.minute + "'"}</span>
                <i class="bi ${icon} me-2"></i>
                <strong>${event.playerName || ''}</strong>
                <span class="event-type-label ${eventTypeClass} ms-2">${formatEventType(eventType)}</span>
                <span class="badge bg-dark ms-2">${teamAbbr || (isHome ? 'HOM' : 'AWY')}</span>
            </div>`;
    });

    return `<div class="event-timeline">${eventsHtml || '<p class="text-muted">No significant events yet</p>'}</div>`;
}

// Render Commentary Tab
function renderCommentaryTab(match, events) {
    const homeAbbr = match.homeTeam.shortName || match.homeTeam.name.substring(0, 3).toUpperCase();
    const awayAbbr = match.awayTeam.shortName || match.awayTeam.name.substring(0, 3).toUpperCase();

    // Sort events by minute (most recent first for commentary style)
    const sortedEvents = [...events].sort((a, b) => (b.minute || 0) - (a.minute || 0));

    let commentaryHtml = '';
    sortedEvents.forEach(event => {
        const eventType = event.type.name || event.type;
        const isHome = event.teamId === match.homeTeam.id;
        const teamAbbr = isHome ? homeAbbr : awayAbbr;
        const minute = event.displayTime || (event.minute + "'");
        const playerName = event.playerName || 'Unknown';

        let commentText = '';
        let commentIcon = '';
        let commentClass = '';

        switch(eventType) {
            case 'GOAL':
                commentText = `⚽ GOAL! ${playerName} scores for ${teamAbbr}!`;
                commentClass = 'commentary-goal';
                break;
            case 'PENALTY_SCORED':
                commentText = `⚽ PENALTY SCORED! ${playerName} converts from the spot for ${teamAbbr}!`;
                commentClass = 'commentary-goal';
                break;
            case 'PENALTY_MISSED':
                commentText = `❌ Penalty missed by ${playerName} (${teamAbbr})`;
                commentClass = 'commentary-miss';
                break;
            case 'OWN_GOAL':
                commentText = `😱 Own goal by ${playerName} (${teamAbbr})`;
                commentClass = 'commentary-goal';
                break;
            case 'YELLOW_CARD':
                commentText = `🟨 Yellow card shown to ${playerName} (${teamAbbr})`;
                commentClass = 'commentary-yellow';
                break;
            case 'RED_CARD':
                commentText = `🟥 RED CARD! ${playerName} (${teamAbbr}) is sent off!`;
                commentClass = 'commentary-red';
                break;
            case 'CORNER':
                commentText = `🚩 Corner kick for ${teamAbbr}`;
                commentClass = 'commentary-corner';
                break;
            case 'FOUL':
                commentText = `⚠️ Foul by ${teamAbbr}`;
                commentClass = 'commentary-foul';
                break;
            case 'OFFSIDE':
                commentText = `🚫 Offside called against ${teamAbbr}`;
                commentClass = 'commentary-offside';
                break;
            case 'SHOT':
                commentText = `💨 Shot by ${playerName} (${teamAbbr})`;
                commentClass = 'commentary-shot';
                break;
            case 'SHOT_ON_TARGET':
                commentText = `🎯 Shot on target by ${playerName} (${teamAbbr})`;
                commentClass = 'commentary-shot';
                break;
            case 'SAVE':
                commentText = `🧤 Save by ${playerName} (${teamAbbr})`;
                commentClass = 'commentary-save';
                break;
            case 'VAR_CHECK':
                commentText = `📺 VAR check in progress...`;
                commentClass = 'commentary-var';
                break;
            case 'SUBSTITUTION':
                commentText = `🔄 Substitution for ${teamAbbr}`;
                commentClass = 'commentary-sub';
                break;
            case 'HALF_TIME':
                commentText = `⏸️ Half time whistle`;
                commentClass = 'commentary-break';
                break;
            case 'FULL_TIME':
                commentText = `🏁 Full time!`;
                commentClass = 'commentary-break';
                break;
            default:
                commentText = `${eventType.replace(/_/g, ' ')} - ${teamAbbr}`;
                commentClass = 'commentary-default';
        }

        commentaryHtml += `
            <div class="commentary-item ${commentClass}">
                <span class="commentary-minute">${minute}</span>
                <span class="commentary-text">${commentText}</span>
            </div>`;
    });

    return `<div class="commentary-container">${commentaryHtml || '<p class="text-muted">No commentary yet</p>'}</div>`;
}

// Render Statistics Tab (ESPN-style)
function renderStatsTab(match, events) {
    const stats = calculateMatchStats(match, events);
    return `
        <div class="stats-container">
            ${renderStatBar('Possession', stats.homePossession, stats.awayPossession, '%')}
            ${renderStatBar('Shots', stats.homeShots, stats.awayShots)}
            ${renderStatBar('Shots on Target', stats.homeShotsOnTarget, stats.awayShotsOnTarget)}
            ${renderStatBar('Corners', stats.homeCorners, stats.awayCorners)}
            ${renderStatBar('Fouls', stats.homeFouls, stats.awayFouls)}
            ${renderStatBar('Yellow Cards', stats.homeYellows, stats.awayYellows)}
            ${renderStatBar('Red Cards', stats.homeReds, stats.awayReds)}
            ${renderStatBar('Offsides', stats.homeOffsides, stats.awayOffsides)}
            ${renderStatBar('Passes', stats.homePasses, stats.awayPasses)}
            ${renderStatBar('Pass Accuracy', stats.homePassAccuracy, stats.awayPassAccuracy, '%')}
        </div>`;
}

function renderStatBar(label, homeVal, awayVal, suffix = '') {
    const total = homeVal + awayVal || 1;
    const homePercent = Math.round((homeVal / total) * 100);
    const awayPercent = 100 - homePercent;
    const homeWins = homeVal > awayVal;
    const awayWins = awayVal > homeVal;

    return `
        <div class="stat-row">
            <div class="stat-value ${homeWins ? 'winner' : ''}">${homeVal}${suffix}</div>
            <div class="stat-bar-container">
                <div class="stat-label">${label}</div>
                <div class="stat-bar">
                    <div class="stat-bar-home ${homeWins ? 'winner' : ''}" style="width: ${homePercent}%"></div>
                    <div class="stat-bar-away ${awayWins ? 'winner' : ''}" style="width: ${awayPercent}%"></div>
                </div>
            </div>
            <div class="stat-value ${awayWins ? 'winner' : ''}">${awayVal}${suffix}</div>
        </div>`;
}

function calculateMatchStats(match, events) {
    // Calculate stats from events
    let stats = {
        homeShots: 0, awayShots: 0,
        homeShotsOnTarget: 0, awayShotsOnTarget: 0,
        homeCorners: 0, awayCorners: 0,
        homeFouls: 0, awayFouls: 0,
        homeYellows: 0, awayYellows: 0,
        homeReds: 0, awayReds: 0,
        homeOffsides: 0, awayOffsides: 0,
        homePasses: 0, awayPasses: 0,
        homePossession: 50, awayPossession: 50,
        homePassAccuracy: 0, awayPassAccuracy: 0
    };

    events.forEach(e => {
        const type = e.type.name || e.type;
        const isHome = e.teamId === match.homeTeam.id;

        if (type === 'SHOT' || type === 'SHOT_OFF_TARGET') {
            if (isHome) stats.homeShots++; else stats.awayShots++;
        }
        if (type === 'SHOT_ON_TARGET' || type === 'GOAL' || type === 'SAVE') {
            if (isHome) { stats.homeShots++; stats.homeShotsOnTarget++; }
            else { stats.awayShots++; stats.awayShotsOnTarget++; }
        }
        if (type === 'CORNER') {
            if (isHome) stats.homeCorners++; else stats.awayCorners++;
        }
        if (type === 'FOUL') {
            if (isHome) stats.homeFouls++; else stats.awayFouls++;
        }
        if (type === 'YELLOW_CARD') {
            if (isHome) stats.homeYellows++; else stats.awayYellows++;
        }
        if (type === 'RED_CARD') {
            if (isHome) stats.homeReds++; else stats.awayReds++;
        }
        if (type === 'OFFSIDE') {
            if (isHome) stats.homeOffsides++; else stats.awayOffsides++;
        }
        if (type === 'PASS') {
            if (isHome) stats.homePasses++; else stats.awayPasses++;
        }
    });

    // Estimate possession based on passes
    const totalPasses = stats.homePasses + stats.awayPasses;
    if (totalPasses > 0) {
        stats.homePossession = Math.round((stats.homePasses / totalPasses) * 100);
        stats.awayPossession = 100 - stats.homePossession;
    } else {
        // Use team strength as fallback
        const homeStr = match.homeTeam.strength?.overall || 75;
        const awayStr = match.awayTeam.strength?.overall || 75;
        stats.homePossession = Math.round((homeStr / (homeStr + awayStr)) * 100);
        stats.awayPossession = 100 - stats.homePossession;
    }

    // Estimate pass accuracy
    stats.homePassAccuracy = Math.round(70 + Math.random() * 20);
    stats.awayPassAccuracy = Math.round(70 + Math.random() * 20);

    // Generate some baseline stats if events are sparse
    if (stats.homeShots === 0 && stats.awayShots === 0) {
        stats.homeShots = Math.floor(Math.random() * 8) + 5;
        stats.awayShots = Math.floor(Math.random() * 8) + 5;
        stats.homeShotsOnTarget = Math.floor(stats.homeShots * 0.4);
        stats.awayShotsOnTarget = Math.floor(stats.awayShots * 0.4);
    }
    if (stats.homeCorners === 0 && stats.awayCorners === 0) {
        stats.homeCorners = Math.floor(Math.random() * 6) + 2;
        stats.awayCorners = Math.floor(Math.random() * 6) + 2;
    }
    if (stats.homeFouls === 0 && stats.awayFouls === 0) {
        stats.homeFouls = Math.floor(Math.random() * 8) + 6;
        stats.awayFouls = Math.floor(Math.random() * 8) + 6;
    }
    if (stats.homePasses === 0 && stats.awayPasses === 0) {
        stats.homePasses = Math.floor(Math.random() * 200) + 300;
        stats.awayPasses = Math.floor(Math.random() * 200) + 300;
    }

    return stats;
}

// Render Predictions Tab
function renderPredictionsTab(match, events) {
    const predictions = calculatePredictions(match, events);

    return `
        <div class="predictions-container">
            <div class="prediction-section">
                <h6 class="prediction-title"><i class="bi bi-trophy me-2"></i>Win Probability</h6>
                <div class="win-probability">
                    <div class="prob-team">
                        <span class="prob-label">${match.homeTeam.shortName}</span>
                        <div class="prob-bar-container">
                            <div class="prob-bar home" style="width: ${predictions.homeWin}%"></div>
                        </div>
                        <span class="prob-value">${predictions.homeWin}%</span>
                    </div>
                    <div class="prob-team draw">
                        <span class="prob-label">Draw</span>
                        <div class="prob-bar-container">
                            <div class="prob-bar draw" style="width: ${predictions.draw}%"></div>
                        </div>
                        <span class="prob-value">${predictions.draw}%</span>
                    </div>
                    <div class="prob-team">
                        <span class="prob-label">${match.awayTeam.shortName}</span>
                        <div class="prob-bar-container">
                            <div class="prob-bar away" style="width: ${predictions.awayWin}%"></div>
                        </div>
                        <span class="prob-value">${predictions.awayWin}%</span>
                    </div>
                </div>
            </div>

            <div class="prediction-section">
                <h6 class="prediction-title"><i class="bi bi-bullseye me-2"></i>Predicted Score</h6>
                <div class="predicted-score">
                    <span class="team-name">${match.homeTeam.shortName}</span>
                    <span class="score">${predictions.predictedHomeGoals}</span>
                    <span class="separator">-</span>
                    <span class="score">${predictions.predictedAwayGoals}</span>
                    <span class="team-name">${match.awayTeam.shortName}</span>
                </div>
                <p class="confidence">Confidence: ${predictions.scoreConfidence}%</p>
            </div>

            <div class="prediction-section">
                <h6 class="prediction-title"><i class="bi bi-flag me-2"></i>Corners Prediction</h6>
                <div class="corners-prediction">
                    <div class="corner-team">
                        <span>${match.homeTeam.shortName}</span>
                        <span class="corner-value">${predictions.homeCorners}</span>
                    </div>
                    <div class="corner-total">
                        <span>Total</span>
                        <span class="corner-value">${predictions.totalCorners}</span>
                    </div>
                    <div class="corner-team">
                        <span>${match.awayTeam.shortName}</span>
                        <span class="corner-value">${predictions.awayCorners}</span>
                    </div>
                </div>
            </div>

            <div class="prediction-section">
                <h6 class="prediction-title"><i class="bi bi-lightning me-2"></i>Event Likelihood</h6>
                <div class="event-predictions">
                    ${renderEventPrediction('Both Teams to Score', predictions.btts)}
                    ${renderEventPrediction('Over 2.5 Goals', predictions.over25)}
                    ${renderEventPrediction('Over 3.5 Goals', predictions.over35)}
                    ${renderEventPrediction('Clean Sheet Home', predictions.cleanSheetHome)}
                    ${renderEventPrediction('Clean Sheet Away', predictions.cleanSheetAway)}
                    ${renderEventPrediction('First Half Goals', predictions.firstHalfGoals)}
                    ${renderEventPrediction('Red Card', predictions.redCard)}
                    ${renderEventPrediction('Penalty', predictions.penalty)}
                </div>
            </div>
        </div>`;
}

function renderEventPrediction(label, probability) {
    const colorClass = probability >= 70 ? 'high' : probability >= 40 ? 'medium' : 'low';
    return `
        <div class="event-pred-row">
            <span class="event-pred-label">${label}</span>
            <div class="event-pred-bar-container">
                <div class="event-pred-bar ${colorClass}" style="width: ${probability}%"></div>
            </div>
            <span class="event-pred-value ${colorClass}">${probability}%</span>
        </div>`;
}

function calculatePredictions(match, events) {
    const homeStr = match.homeTeam.strength?.overall || 75;
    const awayStr = match.awayTeam.strength?.overall || 75;
    const homeAtk = match.homeTeam.strength?.attack || 75;
    const awayAtk = match.awayTeam.strength?.attack || 75;
    const homeDef = match.homeTeam.strength?.defense || 75;
    const awayDef = match.awayTeam.strength?.defense || 75;

    // Home advantage factor
    const homeAdvantage = 1.15;
    const adjustedHomeStr = homeStr * homeAdvantage;

    // Win probabilities
    const totalStr = adjustedHomeStr + awayStr;
    let homeWin = Math.round((adjustedHomeStr / totalStr) * 60);
    let awayWin = Math.round((awayStr / totalStr) * 60);
    let draw = 100 - homeWin - awayWin;

    // Normalize
    if (draw < 15) { draw = 15; homeWin -= 5; awayWin -= 5; }
    if (draw > 35) { draw = 35; homeWin += 5; awayWin += 5; }

    // Predicted goals
    const homeExpectedGoals = ((homeAtk / 100) * (1 - awayDef / 150) * 2.5).toFixed(1);
    const awayExpectedGoals = ((awayAtk / 100) * (1 - homeDef / 150) * 2.0).toFixed(1);
    const predictedHomeGoals = Math.round(parseFloat(homeExpectedGoals));
    const predictedAwayGoals = Math.round(parseFloat(awayExpectedGoals));

    // Corners prediction
    const homeCorners = Math.round(4 + (homeAtk / 100) * 4);
    const awayCorners = Math.round(4 + (awayAtk / 100) * 4);
    const totalCorners = homeCorners + awayCorners;

    // Event likelihoods
    const btts = Math.round(40 + (homeAtk + awayAtk - homeDef - awayDef) / 4);
    const over25 = Math.round(45 + (homeAtk + awayAtk) / 10 - (homeDef + awayDef) / 15);
    const over35 = Math.round(over25 * 0.6);
    const cleanSheetHome = Math.round(20 + homeDef / 5 - awayAtk / 8);
    const cleanSheetAway = Math.round(15 + awayDef / 5 - homeAtk / 8);
    const firstHalfGoals = Math.round(55 + (homeAtk + awayAtk) / 20);
    const redCard = Math.round(8 + Math.random() * 10);
    const penalty = Math.round(12 + Math.random() * 10);

    return {
        homeWin: Math.min(Math.max(homeWin, 10), 80),
        draw: Math.min(Math.max(draw, 10), 40),
        awayWin: Math.min(Math.max(awayWin, 10), 80),
        predictedHomeGoals,
        predictedAwayGoals,
        scoreConfidence: Math.round(35 + Math.random() * 25),
        homeCorners,
        awayCorners,
        totalCorners,
        btts: Math.min(Math.max(btts, 20), 85),
        over25: Math.min(Math.max(over25, 25), 80),
        over35: Math.min(Math.max(over35, 10), 55),
        cleanSheetHome: Math.min(Math.max(cleanSheetHome, 10), 50),
        cleanSheetAway: Math.min(Math.max(cleanSheetAway, 5), 45),
        firstHalfGoals: Math.min(Math.max(firstHalfGoals, 40), 80),
        redCard: Math.min(Math.max(redCard, 5), 25),
        penalty: Math.min(Math.max(penalty, 8), 30)
    };
}

// Helper functions
function getEventClass(type) {
    const classes = {
        'GOAL': 'goal', 'PENALTY_SCORED': 'penalty', 'OWN_GOAL': 'goal',
        'YELLOW_CARD': 'yellow-card', 'RED_CARD': 'red-card',
        'VAR_CHECK': 'var', 'PENALTY_MISSED': 'penalty'
    };
    return classes[type] || '';
}

function getEventIcon(type) {
    const icons = {
        'GOAL': 'bi-bullseye', 'PENALTY_SCORED': 'bi-bullseye', 'OWN_GOAL': 'bi-bullseye',
        'YELLOW_CARD': 'bi-square-fill text-warning', 'RED_CARD': 'bi-square-fill text-danger',
        'VAR_CHECK': 'bi-tv', 'PENALTY_MISSED': 'bi-x-circle'
    };
    return icons[type] || 'bi-circle';
}

function formatEventType(type) {
    return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
}

function getEventTypeClass(type) {
    const classes = {
        'GOAL': 'event-goal',
        'PENALTY_SCORED': 'event-goal',
        'OWN_GOAL': 'event-own-goal',
        'YELLOW_CARD': 'event-yellow',
        'RED_CARD': 'event-red',
        'VAR_CHECK': 'event-var',
        'PENALTY_MISSED': 'event-penalty-missed'
    };
    return classes[type] || '';
}

