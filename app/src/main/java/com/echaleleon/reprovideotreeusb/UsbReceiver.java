package com.echaleleon.reprovideotreeusb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class UsbReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            // El sistema ha montado un nuevo dispositivo
            Toast.makeText(context, "Dispositivo USB detectado", Toast.LENGTH_SHORT).show();

            // Enviamos un broadcast local para avisar a la MainActivity
            Intent refreshIntent = new Intent("REFRESCAR_USB");
            context.sendBroadcast(refreshIntent);
        }
    }
}