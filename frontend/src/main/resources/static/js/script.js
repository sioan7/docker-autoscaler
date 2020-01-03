let file = null;
let fileId = null;
let filename = null;
let checkResultIntervalId = null;
let checkSortStatusIntervalId = null;
let downloadStatusIntervalId = null;

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
        fileId = response.fileId;
        filename = response.filename;
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
            const el = document.getElementById('numberResult');
            el.textContent = 'Number of numbers: ' + result;
            el.style.visibility = 'visible';
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
            downloadStatusIntervalId = setInterval(checkDownloadStatus, 1000);
        }
    });
}

function checkDownloadStatus() {
    fetch('http://localhost:8080/downloadStatus?fileId=' + fileId + ';filename=' + filename, {method: 'GET'})
    .then((r) => r.json()).then((response) => {
        const result = response.value;
        if (result) {
            clearInterval(downloadStatusIntervalId);
            document.getElementById('currentStatus').textContent = 'Current status: DONE';
            const el = document.getElementById('downloadLink');
            el.href = '/download?fileId=' + fileId + ';filename=' + filename;
            el.style.visibility = 'visible';
        }
    });
}
