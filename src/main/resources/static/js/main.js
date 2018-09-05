// 选择用户和房间的样式效果
function selectNode(target) {
    target.addEventListener('dblclick', selectUser2Conn, false);
    // 加些选中效果
    target.addEventListener('mouseenter', function () {
        target.className = 'light';
    }, false);
    target.addEventListener('mouseleave', function () {
        target.className = 'dark';
    }, false);
}