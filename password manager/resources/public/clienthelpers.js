
//copy text to clipboard functionality
function copyToClipboard(text) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(function() {
            console.log('Text copied to clipboard');
        }).catch(function(err) {
            console.error('Failed to copy text: ', err);
        });
    } else {
        // Fallback for browsers that do not support the Clipboard API
        var textarea = document.createElement('textarea');
        textarea.value = text;
        document.body.appendChild(textarea);
        textarea.select();
        try {
            document.execCommand('copy');
            console.log('Text copied to clipboard');
        } catch (err) {
            console.error('Failed to copy text: ', err);
        }
        document.body.removeChild(textarea);
    }
}

//alternating list bg colors - unimplemented
const items = document.querySelectorAll('.password-list');

items.forEach((item, index) => {
  item.style.backgroundColor = index % 2 === 0 ? '#f0f0f0' : '#ffffff';
});