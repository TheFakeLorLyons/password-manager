const cursorEffect = document.getElementById('cursor-effect');

document.addEventListener('mousemove', (e) => {
    cursorEffect.style.width = '100px';
    cursorEffect.style.height = '100px';
    cursorEffect.style.background = 'radial-gradient(rgba(223, 232, 255, 0.95) 80%,rgba(41, 12, 53, .05) 10%)';
    cursorEffect.style.left = `${e.pageX - 50}px`;
    cursorEffect.style.top = `${e.pageY - 50}px`;
    cursorEffect.style.display = 'block'; 
});

document.addEventListener('mouseout', () => {
    cursorEffect.style.width = '0';
    cursorEffect.style.height = '0';
});