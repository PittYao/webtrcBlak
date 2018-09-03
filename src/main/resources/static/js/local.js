'use strict';

let localConnection; // 本地节点
let remoteConnection;// 远端节点
let sendChannel; // 发送消息通道
let receiveChannel;

const dataChannelSend = document.querySelector('textarea#dataChannelSend');
const dataChannelReceive = document.querySelector('textarea#dataChannelReceive');
const startButton = document.querySelector('button#startButton');
const sendButton = document.querySelector('button#sendButton');
const closeButton = document.querySelector('button#closeButton');

const bitrateDiv = document.querySelector('div#bitrate');
const fileInput = document.querySelector('input#fileInput');
const downloadAnchor = document.querySelector('a#download');
const sendProgress = document.querySelector('progress#sendProgress');
const receiveProgress = document.querySelector('progress#receiveProgress');
const statusMessage = document.querySelector('span#status');


let receiveBuffer = [];// 接收数据
let receivedSize = 0;// 接收数据大小

let sendBuffer = [];
let sendSize = 0;

let bytesPrev = 0;
let timestampPrev = 0;
let timestampStart;
let statsInterval = null;
let bitrateMax = 0;


fileInput.addEventListener('change', handleFileInputChange, false);

async function handleFileInputChange() {
    let file = fileInput.files[0];
    if (!file) {
        console.log('No file chosen');
    } else {
        sendDataFile()
    }
}

const localVideo = document.getElementById('localVideo');

const remoteVideo = document.getElementById('remoteVideo');

localVideo.addEventListener('loadedmetadata', function () {
    console.log(`Local video videoWidth: ${this.videoWidth} px,  videoHeight: ${this.videoHeight} px`);
});

let startTime;
let localStream; // 本机音视频流

startButton.onclick = createConnection;
sendButton.onclick = sendData;
closeButton.onclick = closeDataChannels;

fileInput.disabled = true;


// + remoteVideo start
remoteVideo.addEventListener('loadedmetadata', function () {
    console.log(`Remote video videoWidth: ${this.videoWidth}px,  videoHeight: ${this.videoHeight}px`);
});
remoteVideo.onresize = () => {
    console.log(`Remote video size changed to ${remoteVideo.videoWidth}x${remoteVideo.videoHeight}`);
    if (startTime) {
        const elapsedTime = window.performance.now() - startTime;
        console.log('Setup time: ' + elapsedTime.toFixed(3) + 'ms');
        startTime = null;
    }
};
// + remoteVideo end

function enableStartButton() {
    startButton.disabled = false;
}

function disableSendButton() {
    sendButton.disabled = true;
}
// 获取本机的音视频流到video标签
function gotStream(stream) {
    console.log('Received local stream');
    localVideo.srcObject = stream;
    localStream = stream;
}

console.log('Requesting local stream');
navigator.mediaDevices
    .getUserMedia({
        audio: true,
    })
    .then(gotStream)
    .catch(e => alert(`getUserMedia() error: ${e.name}`));


/**
 * 创建连接
 */
function createConnection() {
    startTime = window.performance.now();
    const audioTracks = localStream.getAudioTracks();// 获取音频轨道

    if (audioTracks.length > 0) {
        console.log(`Using audio device: ${audioTracks[0].label}`);
    }
    // TODO  用户信息测试start
    // 发送socket
    window.ws = new WebSocket("wss://" + location.host + "/MyWebsocket/local");

    //　socket接收消息
    ws.onmessage = function (e) {
        var temp = e.data.replace("setRemoteDescription2:", "");
        // 查看获取的远端sdp
        if (temp != e.data) {
            console.log(new Date().toString() + " 接收到remote端的sdp  :  " + e.data);
            localConnection.setRemoteDescription(JSON.parse(temp));
        }

        temp = e.data.replace("addIceCandidate2:", "")
        if (temp != e.data) {
            console.log(new Date().toString() + " 接收到remote端的ice  :  " + e.data);
            var candidate = JSON.parse(temp)

            localConnection.addIceCandidate(candidate)
                .then(
                    () => onAddIceCandidateSuccess(),
                    err => onAddIceCandidateError(null, err)
                );
        }
    };
    ws.onopen = function () {

        dataChannelSend.placeholder = '';
        const servers = null;
        window.localConnection = localConnection = new RTCPeerConnection();
        console.log('Created local peer connection object localConnection');


        sendChannel = localConnection.createDataChannel('sendDataChannel');
        console.log('Created send data channel');

        // 发送本机sdp时会发送本机的ice到远端
        localConnection.onicecandidate = e => {
            console.log(new Date().toString());
            onIceCandidate(localConnection, e);
        };


        sendChannel.binaryType = 'arraybuffer';
        console.log('Created send data channel');


        sendChannel.onopen = onSendChannelStateChange;
        sendChannel.onclose = onSendChannelStateChange;
        // pc2.ontrack = gotRemoteStream;

        localStream.getTracks().forEach(track => localConnection.addTrack(track, localStream));
        console.log('Added local stream to pc1');

        console.log("本地节点创建offer")
        localConnection.createOffer().then(
            gotDescription1,
            onCreateSessionDescriptionError
        );

        // 先获取了远端流
        localConnection.ontrack = gotRemoteStream;
        // 再打开了通道
        localConnection.ondatachannel = receiveChannelCallback;

        startButton.disabled = true;
        closeButton.disabled = false;

        fileInput.disabled = false;
    }
}


function receiveChannelCallback(event) {
    console.log('Receive Channel Callback');
    receiveChannel = event.channel;
    receiveChannel.binaryType = 'arraybuffer';
    receiveChannel.onmessage = onReceiveMessageCallback;
    receiveChannel.onopen = onReceiveChannelStateChange;
    receiveChannel.onclose = onReceiveChannelStateChange;


    receivedSize = 0;
    bitrateMax = 0;
    downloadAnchor.textContent = '';
    downloadAnchor.removeAttribute('download');
    if (downloadAnchor.href) {
        URL.revokeObjectURL(downloadAnchor.href);
        downloadAnchor.removeAttribute('href');
    }
}

//字符编码数值对应的存储长度：
String.prototype.getBytesLength = function () {
    var totalLength = 0;
    var charCode;
    for (var i = 0; i < this.length; i++) {
        charCode = this.charCodeAt(i);
        if (charCode < 0x007f) {
            totalLength++;
        } else if ((0x0080 <= charCode) && (charCode <= 0x07ff)) {
            totalLength += 2;
        } else if ((0x0800 <= charCode) && (charCode <= 0xffff)) {
            totalLength += 3;
        } else {
            totalLength += 4;
        }
    }
    return totalLength;
}
var fileName = '';
function onReceiveMessageCallback(event) {
    console.log(`Received Message ${event.data}`);
    if (typeof  event.data == "string"){
        if (!event.data.startsWith("fileName:")) {
            dataChannelReceive.value += event.data;
        }else {
            fileName = event.data.substring(event.data.indexOf(":")+1);
        }
        receivedSize += event.data.getBytesLength();
        console.log("receivedSize  :" + receivedSize);
        receivedSize = 0;
    } else {
        // 追加发送过来的谈话内容添加到文办框
        receiveBuffer.push(event.data);
        console.log(event.data);
        receivedSize += event.data.byteLength;
        console.log("receivedFileSize  :" + receivedSize);
    }
    // 修改进度条的值
    receiveProgress.value = receivedSize;

    const file = fileInput.files[0];
    // 接收到远端文件
    console.log(event.data.byteLength);
    if ( event.data.byteLength) {
        const received = new Blob(receiveBuffer);
        // receiveBuffer = [];
        // 下载链接
        downloadAnchor.href = URL.createObjectURL(received);
        downloadAnchor.download = fileName;
        downloadAnchor.textContent =
            `download file  '${fileName}' (${receivedSize} bytes)`;
        downloadAnchor.style.display = 'block';

        // 下载速度
        const bitrate = Math.round(receivedSize * 8 /
            ((new Date()).getTime() - timestampStart));
        bitrateDiv.innerHTML
            = `<strong>Average Bitrate:</strong> ${bitrate} kbits/sec (max: ${bitrateMax} kbits/sec)`;

        if (statsInterval) {
            clearInterval(statsInterval);
            statsInterval = null;
        }
        // closeDataChannels();
    }
    window.setTimeout("clearProgessValue(receiveProgress)",3000);
}

async function onReceiveChannelStateChange() {
    const readyState = receiveChannel.readyState;
    console.log(`Receive channel state is: ${readyState}`);
    if (readyState === 'open') {
        timestampStart = (new Date()).getTime();
        timestampPrev = timestampStart;
        statsInterval = setInterval(displayStats, 500);
        await displayStats();
    }
}

async function displayStats() {
    if (remoteConnection && remoteConnection.iceConnectionState === 'connected') {
        const stats = await remoteConnection.getStats();
        let activeCandidatePair;
        stats.forEach(report => {
            if (report.type === 'transport') {
                activeCandidatePair = stats.get(report.selectedCandidatePairId);
            }
        });
        if (activeCandidatePair) {
            if (timestampPrev === activeCandidatePair.timestamp) {
                return;
            }
            // calculate current bitrate
            const bytesNow = activeCandidatePair.bytesReceived;
            const bitrate = Math.round((bytesNow - bytesPrev) * 8 /
                (activeCandidatePair.timestamp - timestampPrev));
            bitrateDiv.innerHTML = `<strong>Current Bitrate:</strong> ${bitrate} kbits/sec`;
            timestampPrev = activeCandidatePair.timestamp;
            bytesPrev = bytesNow;
            if (bitrate > bitrateMax) {
                bitrateMax = bitrate;
            }
        }
    }
}

// + gotRemoteStream
function gotRemoteStream(e) {
    if (remoteVideo.srcObject !== e.streams[0]) {
        remoteVideo.srcObject = e.streams[0];
        console.log('pc2 received remote stream');
    }
}

function sendDataFile() {
    if (fileInput.files.length == 0) {
        return
    }
    const file = fileInput.files[0];
    console.log(`File is ${[file.name, file.size, file.type, file.lastModified].join(' ')}`);

    // Handle 0 size files.
    statusMessage.textContent = '';
    downloadAnchor.textContent = '';
    if (file.size === 0) {
        bitrateDiv.innerHTML = '';
        statusMessage.textContent = 'File is empty, please select a non-empty file';
        closeDataChannels();
        return;
    }
    sendProgress.max = file.size;
    // receiveProgress.max = file.size;
    const chunkSize = 2000;
    const reader = new FileReader();
    let offset = 0;
    let flag = true; // 标识只发送一次文件名
    reader.addEventListener('load', e => {
        // 只发送一次文件名
        if (flag){
            sendChannel.send("fileName:"+file.name);
            flag = false;
        }
        // 发送文件流
        console.log('FileRead.onload ', e);
        sendChannel.send(e.target.result);
        offset += e.target.result.byteLength;
        sendProgress.value = offset;
        if (offset < file.size) {
            readSlice(offset);
        }
    });
    const readSlice = o => {
        console.log('readSlice ', o);
        const slice = file.slice(offset, o + chunkSize);
        reader.readAsArrayBuffer(slice);
    };
    readSlice(0);
}

function onCreateSessionDescriptionError(error) {
    console.log('Failed to create session description: ' + error.toString());
}

function clearProgessValue(progess) {
    progess.value = 0;
}

function sendData() {
    const data = dataChannelSend.value;
    console.log('Sent Data: ' + data);

    sendSize += data.getBytesLength();
    console.log("sendSize: " + sendSize);
    sendProgress.value = sendSize;

    sendChannel.send(data);

    //设置定时清空send进度条
    window.setTimeout("clearProgessValue(sendProgress)", 3000);
}

function closeDataChannels() {
    console.log('Closing data channels');
    sendChannel.close();
    console.log('Closed data channel with label: ' + sendChannel.label);
    // receiveChannel.close();
    // console.log('Closed data channel with label: ' + receiveChannel.label);
    localConnection.close();
    // remoteConnection.close();
    localConnection = null;
    // remoteConnection = null;
    console.log('Closed peer connections');
    startButton.disabled = false;
    sendButton.disabled = true;
    closeButton.disabled = true;
    dataChannelSend.value = '';
    // dataChannelReceive.value = '';
    dataChannelSend.disabled = true;
    disableSendButton();
    enableStartButton();
}

function gotDescription1(desc) {
    console.log("设置本地sdp : " + desc);
    localConnection.setLocalDescription(desc);
    console.log(`Offer from localConnection\n${desc.sdp}`);
    if (desc != null) {
        ws.send("setRemoteDescription1:" + JSON.stringify(desc))
    } else {
        ws.send("setRemoteDescription1:" + JSON.stringify(null))
    }
}

function getName(pc) {
    return (pc === localConnection) ? 'localPeerConnection' : 'remotePeerConnection';
}

/*发送ice约束*/
function onIceCandidate(pc, event) {

    if (event.candidate != null) {
        ws.send("addIceCandidate1:" + JSON.stringify(event.candidate))
    } else {
        ws.send("addIceCandidate1:" + JSON.stringify(null))
    }
    console.log(`${getName(pc)} ICE candidate: ${event.candidate ? event.candidate.candidate : '(null)'}`);
}

function onAddIceCandidateSuccess() {
    console.log('AddIceCandidate success.');
}

function onAddIceCandidateError(error) {
    console.log(`Failed to add Ice Candidate:` + JSON.stringify(error));
}


function onSendChannelStateChangeFile() {
    const readyState = sendChannel.readyState;
    console.log(`Send channel state is: ${readyState}`);
    if (readyState === 'open') {
        sendDataFile();
    }
}

function onSendChannelStateChange() {
    const readyState = sendChannel.readyState;
    console.log('Send channel state is: ' + readyState);
    if (readyState === 'open') {
        dataChannelSend.disabled = false;
        dataChannelSend.focus();
        sendButton.disabled = false;
        closeButton.disabled = false;

        onSendChannelStateChangeFile();

    } else {
        console.log('Closing data channels');
        sendChannel.close();
        console.log('Closed data channel with label: ' + sendChannel.label);

        localConnection.close();
        localConnection = null;
        console.log('Closed peer connections');

        startButton.disabled = false;
        sendButton.disabled = true;
        closeButton.disabled = true;

        dataChannelSend.value = '';
        dataChannelSend.disabled = true;

        disableSendButton();
        enableStartButton();
    }
}
