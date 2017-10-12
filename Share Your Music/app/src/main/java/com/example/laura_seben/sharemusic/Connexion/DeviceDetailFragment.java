package com.example.laura_seben.sharemusic.Connexion;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.laura_seben.sharemusic.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    public static final String IP_SERVER = "192.168.49.1";
    public static int PORT = 8988;
    private static boolean server_running = false;

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
                        //                        new DialogInterface.OnCancelListener() {
                        //
                        //                            @Override
                        //                            public void onCancel(DialogInterface dialog) {
                        //                                ((DeviceActionListener) getActivity()).cancelDisconnect();
                        //                            }
                        //                        }
                );
                ((DeviceListFragment.DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Permet à l'utilisateur de selectionner une musique de sa liste de musique
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("audio/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });

        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        //String localIP = Utils.getLocalIPAddress();
        //Trick to find the ip in the file /proc/net/arp
        //String client_mac_fixed = new String(device.deviceAddress).replace("99", "19");
        //String clientIP = Utils.getIPFromMac(client_mac_fixed);

        /**
         * L'host a choisi une musique à envoyer au client
         * Lance FileTransferService
         */

        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WifiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());

        /*if(localIP.equals(IP_SERVER)){
            serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, localIP);//localip ou clientIP
        }else{
            serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, IP_SERVER);
        }*/
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());

        serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
        getActivity().startService(serviceIntent);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());


        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            //Lancement du HostPlayer, on y rajoute le PORT sur lequel on va communiquer
            Intent intent = new Intent(getActivity(),com.example.laura_seben.sharemusic.Musique.test.HostPlayer.class);
            intent.putExtra("PORT",PORT);
            startActivity(intent);

        } else if (info.groupFormed) {
            // The other device acts as the client.
            //Lancement du CLientPlayer, on y rajoute le PORT ainsi que l'adresse du Host, nécessaire pour lui envoyer un message de présence
            Intent intent = new Intent(getActivity(),com.example.laura_seben.sharemusic.Musique.test.ClientPlayer.class);
            intent.putExtra("PORT",PORT);
            String Hostadresse = info.groupOwnerAddress.getHostAddress();
            intent.putExtra("Hostadresse",Hostadresse);


            startActivity(intent);
        }

        // cache le bouton connect
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Met à jour la vue en affichant les détails de l'appareil sélectionné
     * @param device Les informations de l'appareil sélectionné
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Change la vue après une connexion ou une déconnexion
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * Un simple server socket qui accepte les connexions et ecrit des données en streaming
     */

    public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {

        private final Context context;
        private final TextView statusText;


        public ServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                Log.d(WifiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WifiDirectActivity.TAG, "Server: connection done");
                InputStream inputstream = client.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(inputstream);
                bis.mark(1);
                int byte1 = bis.read();
                if(byte1==1) {
                    final File f = new File(Environment.getExternalStorageDirectory() + "/"
                            + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                            + ".mpeg");
                    File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();

                    Log.d(WifiDirectActivity.TAG, "server: copying files " + f.toString());


                    copyFile(bis, new FileOutputStream(f));
                    serverSocket.close();
                    server_running = false;
                    return f.getAbsolutePath();
                }
                else{
                    return "stop";
                }
            } catch (IOException e) {
                Log.e(WifiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);

                intent.setDataAndType(Uri.parse("file://" + result), "audio/*");
                context.startActivity(intent);
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

    }
    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WifiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

}