let file = null;

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
    }).then(response => response.json());
}
