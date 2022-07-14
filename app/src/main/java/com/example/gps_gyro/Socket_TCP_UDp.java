package com.example.gps_gyro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;


class TCPClient {

    //IP地址
    private String address;
    //端口
    private int port;
    //发送内容
    private String msg;
    private Socket socket = null;
    private OutputStream os = null;
    private InputStream is = null;
    private String Data;
    private boolean Normal=false;
    public void setMsg(String msge)
    {
        msg=msge;
    }
    public boolean IsNormal(){return Normal;}

    public void SetIPAndPort(String address, int port) {
        this.address = address;
        this.port = port;
    }
    public void SetTCPClient(String address, int port, String msg) {
        this.address = address;
        this.port = port;
        this.msg = msg;
    }
    public TCPClient(String address, int port, String msg) {
        this.address = address;
        this.port = port;
        this.msg = msg;
    }

    /**
     * 设置
     */
    public void sendSocket() {
        new Thread() {
            public void run () {
                if(socket==null) {
                    //1.创建监听指定服务器地址以及指定服务器监听的端口号
                    //IP地址，端口号
                    try {
                        ShowData("正在连接中\\(^_^)/");
                        Thread.sleep(30);
                        socket = new Socket(address, port);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                        ErrStop("连接出错了~(-_-)~");
                    }
                    if(socket==null || socket.isClosed())
                    {
                        ErrStop("连接失败了~(-_-)~");
                    }
                    else
                    {
                        // 2.拿到客户端的socket对象的输出流发送给服务器数据
                        try {
                            os = socket.getOutputStream();
                            is = socket.getInputStream();
                            SetIS(is);
                            os.write(msg.getBytes(LocationActivity.charset));
                            os.flush();
                            ShowData2(msg);
                        } catch (IOException e) {
                            ErrStop("发送出错了~(-_-)~");
                            e.printStackTrace();
                        }
                        //os.write(msg.getBytes(LocationActivity.charset));
                        //socket.shutdownOutput();
                        //拿到socket的输入流，这里存储的是服务器返回的数据
                    }
                }
                else
                {
                    if(socket==null || socket.isClosed())
                    {
                        ErrStop("连接断开了~(-_-)~");
                    }
                    else
                    {
                        try {
                            os.write(msg.getBytes(LocationActivity.charset));
                            os.flush();
                            ShowData2(msg);
                        } catch (IOException e) {
                            e.printStackTrace();
                            ErrStop("发送出错了~(-_-)~");
                        }
                    }
                }
            }
        }.start();
    }
    public void ReadIO()
    {
        new Thread() {
            public void run() {
                //ShowData("接收框准备完毕！");/////////////////////////////////////////////////////////////////
                ShowErr("连接成功了\\(^_^)/");
                final byte[] buffer = new byte[1024];//创建接收缓冲区
                int num=0;
                while(Normal)
                {
                    if(Normal)
                    {
                        if(num<60000000)
                        {
                            num = num+1;
                        }
                        else
                        {
                            try {
                                socket.sendUrgentData(0xFF);//心跳包
                            } catch (IOException e) {
                                //ErroStop("服务器断开了~(-_-)~");
                                ErrStop("服务器断开了~(-_-)~");
                                e.printStackTrace();
                                break;
                            }
                            num=0;
                        }
                        if(socket==null || socket.isClosed())
                        {
                            ErrStop("中途断开了~(-_-)~");
                        }
                        else
                        {
                            Data = "";
                            try {
                                //if(socket.isInputStreamShutdown()){}
                                final int len = is.read(buffer);
                                int j = len;
                                if(j > 0)
                                {
                                    for(;j<buffer.length;j++){
                                        buffer[j]=0;
                                    }
                                    try {
                                        Data = new String(buffer,LocationActivity.charset);
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    if (Data != null && Data != "") {
                                        ShowData(Data);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else
                    {
                        ErrStop("断开连接了~(-_-)~");
                        break;
                    }
                }
            }

        }.start();
    }
    public void ErrStop(String data)
    {
        Normal = false;
        //MainActivity.ShowErro(data);
        FoceCloseIO();
    }
    public void CloseIO()
    {
        Normal = false;
        try {
            Thread.sleep(10);
        } catch ( InterruptedException ex) {
            ex.printStackTrace();
        }
        FoceCloseIO();
    }
    public void FoceCloseIO()
    {
        Normal = false;
        try {
            if (socket != null)
            {
                //socket.shutdownOutput();

                socket.close();
                Thread.sleep(30);
            }
            socket = null;
        } catch (IOException | InterruptedException ex) {
            ShowErr("关闭数据通道出错!");
            ex.printStackTrace();
        }
    }
    private void SetIS(InputStream iss)
    {
        is=iss;
        Normal = true;
        ReadIO();
    }
    /**
     * 显示消息
     */
    private void ShowData(String data)
    {
        //MainActivity.ShowData(data);
    }
    private void ShowData2(String data)
    {
        //MainActivity.ShowData2(data);
    }
    public void ShowErr(String data)
    {
        //MainActivity.ShowErro(data);
    }
}
class TCPServer {
    private boolean IsNormal;
    private boolean HasStart = false;
    private ServerSocket SSocket = null;
    private Socket[] socket={null,null,null};
    private boolean[] socketEnable={false,false,false};
    private int PORT=8080;
    private String SendData = "";
    private int WhoID = -1;
    public boolean IsIDInRun(int i)
    {
        return socket[i]!=null && socketEnable[i];
    }
    public void SetPort(int i)
    {
        PORT=i;
    }
    public boolean IsRun()
    {
        return IsNormal;
    }
    public void CloseServer()
    {
        IsNormal = false;
        for(int j=0;j<socket.length;j++)
        {
            FoceCloseByID(j);
        }
        SSocket=null;
        HasStart=false;
    }
    public void SendMSGAllID(String data)
    {
        for(int j=0;j<socket.length;j++)
        {
            SendMSGByID(data,j);
        }
        ShowData2(data);
    }
    public void SendMSGByID(String data,int i)
    {
        OutputStream os;
        if(IsIDInRun(i))
        {
            SendData = data;
            WhoID = i ;
            int AutoBreak = 10;
            while(true)
            {
                if(WhoID < 0 || SendData==null || SendData==""){break;}
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(AutoBreak<0)
                {
                    break;
                }
                AutoBreak=AutoBreak-1;
            }
        }
    }
    public void CloseByID(final int i)
    {
        socketEnable[i] = false;
    }
    public int NewAClient()//每次使用后需要考虑是否再加一个来监听，多一个监听可以多连接一个客户端
    {
        if(HasStart==false)
        {
            Start();
            HasStart = true;
        }
        int i = FindAEmptySocket();
        if(i>=0)
        {
            NewThread(i);
            //OutInfo("添加客户成功，客户id: " + String.valueOf(i));
        }
        else
        {
            ShowErr("客户数量达到最大值了.");
        }
        return i;
    }
    private void FoceCloseByID(final int i)
    {
        socketEnable[i] = false;
        if (socket[i] != null)
        {
            //socket.shutdownOutput();
            try {
                socket[i].close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket[i] = null;
    }
    private void Start()
    {
        try {
            SSocket = new ServerSocket(PORT);
            IsNormal = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private int FindAEmptySocket()
    {
        for(int j=0;j<socket.length;j++)
        {
            if(socket[j]==null || socketEnable[j]==false)
            {
                return j;
            }
        }
        return -1;
    }
    private void NewThread(final int i)//不止接受一个客户端
    {
        final OutputStream[] os = {null};
        final InputStream[] is = {null};
        socket[i] = null;//接受一个连接
        final boolean[] IsConect = {false};
        final byte[] buffer = new byte[1024];//创建接收缓冲区
        socketEnable[i]=true;
        new Thread() {
            public void run()
            {
                ShowErr("等待客户连接！");
                while (IsNormal && socketEnable[i])
                {
                    try {
                        socket[i] = SSocket.accept();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        //System.out.println("客户端已连接，ip地址:" + socket[i].getInetAddress().getHostAddress() + "端口号:" + socket[i].getLocalPort());
                        IsConect[0] = true;
                        ShowErr("客户已连接！"+"<br>ip地址:" + socket[i].getInetAddress().getHostAddress() + "<br>端口号:" + socket[i].getLocalPort());
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        is[0] = socket[i].getInputStream();
                        os[0] = socket[i].getOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        os[0].write("Hello!!!".getBytes(LocationActivity.charset));
                        OutThread(os[0],i);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //DataInputStream dis = new DataInputStream();//输入管道
                    //dis.close();
                    //s.close();
                    int num=0;
                    while(socketEnable[i] && IsConect[0])
                    {
                        if(num<60000000)
                        {
                            num = num+1;

                        }
                        else
                        {
                            try {
                                socket[i].sendUrgentData(0xFF);//心跳包
                            } catch (IOException e) {
                                //ErroStop("服务器断开了~(-_-)~");
                                ShowErr("客户断开了~(-_-)~");
                                e.printStackTrace();
                                break;
                            }
                            num=0;
                        }
                        String Data = "";
                        try {
                            //if(socket.isInputStreamShutdown()){}
                            int len = is[0].read(buffer);
                            int j = len;
                            if(j > 0)
                            {
                                for(;j<buffer.length;j++){
                                    buffer[j]=0;
                                }
                                try {
                                    Data = new String(buffer,LocationActivity.charset);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                if (Data != null && Data != "") {
                                    OutInfo("IP:" + socket[i].getInetAddress().getHostAddress()  + "<br>内容:" + Data);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    FoceCloseByID(i);
                    break;
                }
            }
        }.start();
    }
    private void OutThread(final OutputStream os, final int i)
    {
        new Thread() {
            public void run()
            {
                while(socketEnable[i] && IsNormal)
                {
                    if(WhoID == i && SendData!=null)
                    {
                        try {
                            os.write(SendData.getBytes(LocationActivity.charset));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        WhoID=-1;
                        SendData="";
                    }
                }
            }
        }.start();
    }
    private void OutInfo(String data)
    {
        /**
         * 显示消息
         */
        //MainActivity.ShowData(data);
    }
    public void ShowErr(String data)
    {
        //MainActivity.ShowErro(data);
    }
    private void ShowData2(String data)
    {
        //MainActivity.ShowData2(data);
    }
}

class UDPClient {
    private static InetAddress mAddress;
    private static DatagramSocket socket = null;
    private static String ip = "192.168.0.101"; //发送给整个局域网
    private static int SendPort = 8080;  //发送方和接收方需要端口一致
    public static void SetIP(final String content) {ip = content;}
    public static void SetPort(final int content) { SendPort = content;}
    public static void UDPSend(final String content,final String IP,final int Port) {
        //初始化socket
        SetIP(IP);
        SetPort(Port);
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            mAddress = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //创建线程发送信息
        new Thread() {
            private byte[] sendBuf;

            public void run() {
                try {
                    sendBuf = content.getBytes(LocationActivity.charset);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                DatagramPacket recvPacket1 = new DatagramPacket(sendBuf, sendBuf.length, mAddress, SendPort);
                try {
                    socket.send(recvPacket1);
                    socket.close();
                    ShowData2(content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private void ShowData(String data)
    {
        //MainActivity.ShowData(data);
    }
    private static void ShowData2(String data)
    {
        //MainActivity.ShowData2(data);
    }
    public void ShowErr(String data)
    {
        //MainActivity.ShowErro(data);
    }
}
class UDPServer {

    private static int PORT = 8080;
    private byte[] msg = new byte[1024];
    private boolean life = true;
    private DatagramPacket dPacket = new DatagramPacket(msg, msg.length);
    private DatagramSocket dSocket = null;
    public UDPServer() {
    }
    public void SetPort(int i)
    {
        PORT=i;
    }
    public void CloseServer()
    {
        life = false;
    }
    public boolean isLife() {
        return life;
    }
    public void setLife(boolean life) {
        this.life = life;
    }

    public void Start() {
        setLife(true);
        new Thread() {
            public void run () {
                //Log.e("zziafyc", "已将内容发送给了AIUI端内容为：" );
                try {
                    if(dSocket!=null && dSocket.isClosed()==false)
                    {
                        dSocket.close();
                        Thread.sleep(30);
                        dSocket = null;
                    }
                    dSocket = new DatagramSocket(PORT);
                    while (life) {
                        try {
                            for(int j=0;j<msg.length;j++){
                                msg[j]=0;
                            }
                            dSocket.setSoTimeout(1000);
                            dSocket.receive(dPacket);
                            ShowData(new String(msg,LocationActivity.charset));
                            //Log.i("msg sever received", new String(msg));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    //Log.e("zziafyc", "关闭：" );
                    dSocket.close();
                    Thread.sleep(30);
                    dSocket = null;
                } catch (SocketException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private void ShowData(String data)
    {
        //MainActivity.ShowData(data);
    }
    private void ShowData2(String data)
    {
        //MainActivity.ShowData2(data);
    }
    public void ShowErr(String data)
    {
        //MainActivity.ShowErro(data);
    }
}
