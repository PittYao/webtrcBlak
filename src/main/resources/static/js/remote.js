'use strict';

let localConnection;
let remoteConnection;
let sendChannel;
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

const liveUsers = document.querySelector('div#liveUsers');//用户在线列表
const remoteUserP = document.querySelector('p#username');// url的本地用户

let receiveBuffer = [];
let receivedSize = 0;

let sendBuffer = [];
let sendSize = 0;

// let remoteUser = 'remoteUser';
let remoteUser = remoteUserP.innerHTML;
let localUser ;


let bytesPrev = 0;
let timestampPrev = 0;
let timestampStart;
let statsInterval = null;
let bitrateMax = 0;

let startTime;
let localStream;


startButton.onclick = createConnection;
sendButton.onclick = sendData;
closeButton.onclick = closeDataChannels;

fileInput.disabled = true;

const remoteVideo = document.getElementById('remoteVideo');
// + localVideo start
const localVideo = document.getElementById('localVideo');

localVideo.addEventListener('loadedmetadata', function () {
    console.log(`Local video videoWidth: ${this.videoWidth}px,  videoHeight: ${this.videoHeight}px`);
});

function createConnection() {

    window.ws = new WebSocket("wss://" + location.host + "/MyWebsocket/" + remoteUser);
    // + localStream
    const audioTracks = localStream.getAudioTracks();
    if (audioTracks.length > 0) {
        console.log(`Using audio device: ${audioTracks[0].label}`);
    }


    ws.onopen = function () {
        dataChannelSend.placeholder = '';

        window.remoteConnection = remoteConnection = new RTCPeerConnection();
        console.log('Created remote peer connection object remoteConnection');
        // + sendChannel start
        sendChannel = remoteConnection.createDataChannel('sendDataChannel');
        console.log('Created send data channel');

        sendChannel.binaryType = 'arraybuffer';
        console.log('Created send data channel');

        sendChannel.onopen = onSendChannelStateChange;
        sendChannel.onclose = onSendChannelStateChange;
        // + sendChannel end

        // addIce之后才会触发，发送ICE到远端
        remoteConnection.onicecandidate = e => {
            console.log("remote ice");
            onIceCandidate(remoteConnection, e);
        };

        console.log('Added local stream to remote PC ');
        // 添加流到peer
        localStream.getTracks().forEach(track => remoteConnection.addTrack(track, localStream));

        // 先获取了远端流
        remoteConnection.ontrack = gotRemoteStream;
        // 再打开了数据通道
        remoteConnection.ondatachannel = receiveChannelCallback;

        startButton.disabled = false;
        closeButton.disabled = true;
        fileInput.disabled = true;
    }

    ws.onmessage = function (e) {
        let data = e.data;
        console.log(data);
        try {
            if (data.startsWith('liverUsers')) {
                //取下用户列表tag
                let json = data.substring(data.indexOf(":") + 1);
                //转换json
                let liveUsersData = JSON.parse(json);
                console.log(liveUsersData);
                // 展示在线用户列表
                for (let i = 0; i < liveUsersData.length; i++) {
                    let item = liveUsersData[i];
                    // 不显示自己
                    if (item.name == remoteUser) {
                        continue;
                    }
                    let p = document.createElement("p");
                    p.innerHTML = item.name;
                    // 双击发起连接邀请
                    p.addEventListener('dblclick', function () {
                        let descUser = item.name;
                        ws.send("call:" + descUser); // local
                    }, false)
                    liveUsers.appendChild(p);
                }
            }
            // "localUser:" + currentUser + ";" + sdpOrIce
            // 接收local传过来的sdp和ice
            if (data.startsWith('localUser')) {
                // 取下头部
                localUser = data.substring(data.indexOf(":") + 1, data.indexOf(";"));// local
                // 取下sdp或ice
                let sdpOrIce = data.substring(data.indexOf(";") + 1);
                // 对sdp和ice后续操作
                //当服务器发来local端的sdp时，进行操作
                let temp = sdpOrIce.replace("setRemoteDescription1:", "")
                if (temp != sdpOrIce) {
                    console.log(new Date().toString() + "接收到local端的sdp  :  " + sdpOrIce);
                    remoteConnection.setRemoteDescription(JSON.parse(temp));
                    remoteConnection.createAnswer().then(
                        gotDescription2,
                        onCreateSessionDescriptionError
                    );
                }

                temp = sdpOrIce.replace("addIceCandidate1:", "")
                if (temp != sdpOrIce) {
                    console.log(new Date().toString() + "  接收到local端的ice  :  " + sdpOrIce);
                    let candidate = JSON.parse(temp)

                    remoteConnection.addIceCandidate(candidate)
                        .then(
                            () => onAddIceCandidateSuccess(null),
                            err => onAddIceCandidateError(null, err)
                        );
                }
            }
        } catch (e) {
            console.log("接收数据错误: " + e.data);
        }
    };


}


function gotStream(stream) {
    console.log('Received local stream');
    localVideo.srcObject = stream;
    localStream = stream;
    // 创建连接 上线
    createConnection();

}

navigator.mediaDevices
    .getUserMedia({
        audio: true,
    })
    .then(gotStream)
    .catch(e => alert(`getUserMedia() error: ${e.name}`));
// + localVideo end


remoteVideo.addEventListener('loadedmetadata', function () {
    console.log(`Remote video videoWidth: ${this.videoWidth}px,  videoHeight: ${this.videoHeight}px`);
});
remoteVideo.onresize = () => {
    console.log(`Remote video size changed to ${remoteVideo.videoWidth}x${remoteVideo.videoHeight}`);
    // We'll use the first onsize callback as an indication that video has started
    // playing out.
    if (startTime) {
        const elapsedTime = window.performance.now() - startTime;
        console.log('Setup time: ' + elapsedTime.toFixed(3) + 'ms');
        startTime = null;
    }
};

function enableStartButton() {
    startButton.disabled = false;
}

fileInput.addEventListener('change', handleFileInputChange, false);

async function handleFileInputChange() {
    let file = fileInput.files[0];
    if (!file) {
        console.log('No file chosen');
    } else {
        sendDataFile();
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
// 接收消息和文件
let fileName = '';

function onReceiveMessageCallback(event) {
    console.log(`Received Message ${event.data}`);
    if (typeof  event.data == "string") {
        if (!event.data.startsWith("fileName:")) {
            dataChannelReceive.value += event.data;
        } else {
            fileName = event.data.substring(event.data.indexOf(":") + 1);
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
    if (event.data.byteLength) {
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
    window.setTimeout("clearProgessValue(receiveProgress)", 3000);
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

// display bitrate statistics.
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


function gotRemoteStream(e) {
    if (remoteVideo.srcObject !== e.streams[0]) {
        remoteVideo.srcObject = e.streams[0];
        console.log('pc2 received remote stream');
    }
}

function disableSendButton() {
    sendButton.disabled = true;
}


function onSendChannelStateChange() {
    const readyState = sendChannel.readyState;
    console.log('Send channel state is: ' + readyState);
    if (readyState === 'open') {
        dataChannelSend.disabled = false;
        dataChannelSend.focus();
        sendButton.disabled = false;
        closeButton.disabled = false;
        // onSendChannelStateChangeFile();
    } else {
        // dataChannelSend.disabled = true;
        // sendButton.disabled = true;
        // closeButton.disabled = true;

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
    // sendChannel.close();
    // console.log('Closed data channel with label: ' + sendChannel.label);
    receiveChannel.close();
    console.log('Closed data channel with label: ' + receiveChannel.label);
    // localConnection.close();
    // remoteConnection.close();
    // localConnection = null;
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
    const chunkSize = 2000;// 规定每次发送的最大字节是16384
    const reader = new FileReader();
    let offset = 0;
    let flag = true; // 标识只发送一次文件名
    reader.addEventListener('load', e => {
        // 只发送一次文件名
        if (flag) {
            sendChannel.send("fileName:" + file.name);
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

function gotDescription2(desc) {
    remoteConnection.setLocalDescription(desc);
    console.log(`Answer from remoteConnection\n${desc.sdp}`);
    //localUser
    ws.send("localUser:"+localUser+";setRemoteDescription2:" + JSON.stringify(desc))
}

function getOtherPc(pc) {
    return (pc === localConnection) ? remoteConnection : localConnection;
}

function getName(pc) {
    return (pc === localConnection) ? 'localPeerConnection' : 'remotePeerConnection';
}

function onIceCandidate(pc, event) {
    if (event.candidate != null) {
        ws.send("localUser:"+localUser+";addIceCandidate2:" + JSON.stringify(event.candidate))
    } else {
        ws.send("localUser:"+localUser+";addIceCandidate2:" + JSON.stringify(null))
    }
    console.log(`${getName(pc)} ICE candidate: ${event.candidate ? event.candidate.candidate : '(null)'}`);
}

function onAddIceCandidateSuccess() {
    console.log('AddIceCandidate success.');
}

function onAddIceCandidateError(error) {
    console.log(`Failed to add Ice Candidate: ${error.toString()}`);
}
