let file = null;
let fileId = null;
let checkResultIntervalId = null;
let checkSortStatusIntervalId = null;

function openFile(event) {
    const files = event.files;
    if (files && files.length) {
        file = files[0];
    }
}

function uploadFile() {
    const formData = new FormData()
    formData.append('file', file);
    return fetch(`http://localhost:8080/file`, {
        method: 'POST',
        body: formData,
    }).then((r) => r.json()).then(response => {
        fileId = response.value;
        document.getElementById('refreshInformation').style.visibility = 'visible';
        document.getElementById('currentStatus').style.visibility = 'visible';
        checkResultIntervalId = setInterval(checkNumbersResult, 1000);
        checkSortStatusIntervalId = setInterval(checkSortStatus, 1000);
    });
}

function checkNumbersResult() {
    fetch('http://localhost:8080/numbers?fileId=' + fileId, {method: 'GET'}).then((r) => r.json()).then((response) => {
        const result = response.value;
        if (result) {
            document.getElementById('numberResult').textContent = 'Number of numbers: ' + result;
            clearInterval(checkResultIntervalId);
        }
    });
}

function checkSortStatus() {
    fetch('http://localhost:8080/sortStatus?fileId=' + fileId, {method: 'GET'}).then((r) => r.json()).then((response) => {
        const result = response.value;
        document.getElementById('currentStatus').textContent = 'Current status: ' + result;
        if (result === 'merging chunks') {
            clearInterval(checkSortStatusIntervalId);
        }
    });
}
