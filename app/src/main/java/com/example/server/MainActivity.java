package com.example.server;

import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    EditText ip_dialog;
    EditText port_dialog;
    TextView textView;
    Button start_btn;
    Button shutdown_btn;

    EditText input_dialog;
    Button send_btn;

    JSONObject jsonObject;

    private static final int PORT = 20001;
    private static Thread thread;                //執行緒
    private static int serverport = 20001;
    private static ServerSocket serverSocket;    //伺服端的Socket
    private static ArrayList clients = new ArrayList();
    private static int count=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ip_dialog = findViewById(R.id.editTextTextPersonName);
        port_dialog = findViewById(R.id.editTextTextPersonName2);
        textView = findViewById(R.id.textView);
        start_btn = findViewById(R.id.button);
        shutdown_btn = findViewById(R.id.button2);
        input_dialog = findViewById(R.id.input_dialog);
        send_btn = findViewById(R.id.send_btn);
        //shutdown_btn.setOnClickListener(button2);


        //start_btn.setOnClickListener(new Button1ClickListener());
        //取得非UI線程傳來的msg，以改變介面

    }
    public void connect(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(serverport);
                    System.out.println("Server is start.");
                    // 顯示等待客戶端連接
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.append("Server is start." + "\n");
                        }
                    });
                    System.out.println("Waiting for client connect");
                    // 當Server運作中時
                    while (!serverSocket.isClosed()) {
                        // 呼叫等待接受客戶端連接
                        waitNewClient();
                    }
                } catch (IOException e) {
                    System.out.println("Server Socket ERROR");
                }


            }
        }).start();
    }

    public  void waitNewClient() {
        try {
            Socket socket = serverSocket.accept();
            ++count;
            System.out.println("現在使用者個數："+count);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.append("現在使用者個數："+count+ "\n");
                }
            });
            // 呼叫加入新的 Client 端
            addNewClient(socket);

        } catch (IOException e) {
            System.out.println(" 9999");
        }
    }
    String Msg;
    // 加入新的 Client 端
    public void addNewClient(final Socket socket) throws IOException {
        // 以新的執行緒來執行
        final BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        final String client_ip = socket.getRemoteSocketAddress().toString();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 增加新的 Client 端
                    clients.add(socket);
                    // 取得網路串流

                    int first = 0;
                    // 當Socket已連接時連續執行
                    while (socket.isConnected()) {
                        // 取得網路串流的訊息
                        Msg= br.readLine();
                        if(Msg==null){
                            System.out.println("Client Disconnected!");
                            break;
                        }
                        //輸出訊息
                        System.out.println(Msg);
                        jsonObject = new JSONObject(Msg);
                        final String name = jsonObject.getString("Username");
                        //String title = jsonObject.getString("Username");
                        final String msg = jsonObject.getString("Msg");
                        final String connect = jsonObject.getString("Connect");

                        if(connect.equals("0")){
                            System.out.println("Client Disconnected!");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textView.append(name + " Disconnected!" + "\n");
                                }
                            });
                            break;
                        }
                        //String info = jsonObject.getString("Connect");
                        if(first==0){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textView.append("Welcome "+name+"("+ client_ip + ")" + "!\n");
                                }
                            });
                            first++;
                            JSONObject json_write = new JSONObject();
                            json_write.put("Uid", "89");
                            json_write.put("Username", "Server");
                            json_write.put("Msg", "Welcome "+name + "!");
                            json_write.put("Connect", "1");//傳送離線動作給伺服器
                            castMsg(json_write);
                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textView.append(""+name+"("+ client_ip + ")"  + ":" + msg + "\n");
                                }
                            });
                            castMsg(Msg);
                        }

                        // 廣播訊息給其它的客戶端

                    }
                } catch (IOException | JSONException e) {
                    e.getStackTrace();
                }
                finally{
                    // 移除客戶端
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("socket.close()error");
                    }
                    clients.remove(socket);
                    --count;
                    System.out.println("現在使用者個數："+count);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.append("現在使用者個數："+count + "\n");
                        }
                    });
                }
            }
        });

        // 啟動執行緒
        t.start();
    }

    public void shutdown(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json_write = new JSONObject();
                    json_write.put("Uid", "89");
                    json_write.put("Username", "Server");
                    json_write.put("Msg", "Server shutdown!");
                    json_write.put("Connect", "0");//傳送離線動作給伺服器
                    castMsg(json_write);
                    serverSocket.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.append("Server shutdown"+ "\n");
                        }
                    });
                } catch (JSONException | IOException e) {}
                
            }
        }).start();
    }
    public void send(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json_write = new JSONObject();
                    json_write.put("Uid", "89");
                    json_write.put("Username", "Server");
                    json_write.put("Msg", input_dialog.getText().toString());
                    json_write.put("Connect", "1");//傳送離線動作給伺服器
                    castMsg(json_write);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.append("Server:"+input_dialog.getText().toString() + "\n");
                        }

                    });
                } catch (JSONException e) {}

            }
        }).start();
        input_dialog.setText("");
    }
        // 廣播訊息給其它的客戶端
    public static void castMsg(String Msg){
        // 創造socket陣列
        Socket[] clientArrays =new Socket[clients.size()];
        // 將 clients 轉換成陣列存入 clientArrays
        clients.toArray(clientArrays);
        // 走訪 clientArrays 中的每一個元素
        for (Socket socket : clientArrays ) {
            try {
                // 創造網路輸出串流
                BufferedWriter bw;
                bw = new BufferedWriter( new OutputStreamWriter(socket.getOutputStream()));
                // 寫入訊息到串流
                bw.write(Msg+"\n");
                // 立即發送
                bw.flush();
            } catch (IOException e) {}
        }

    }
    public static void castMsg(JSONObject Msg){
        // 創造socket陣列
        Socket[] clientArrays =new Socket[clients.size()];
        // 將 clients 轉換成陣列存入 clientArrays
        clients.toArray(clientArrays);
        // 走訪 clientArrays 中的每一個元素
        for (Socket socket : clientArrays ) {
            try {
                // 創造網路輸出串流
                BufferedWriter bw;
                bw = new BufferedWriter( new OutputStreamWriter(socket.getOutputStream()));
                // 寫入訊息到串流
                bw.write(Msg+"\n");
                // 立即發送
                bw.flush();
            } catch (IOException e) {}
        }
    }
}