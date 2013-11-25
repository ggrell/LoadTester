package com.feigdev.loadtester;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

/**
 * Created by ejf3 on 11/24/13.
 */
public class MainFragment extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
    private TextView cpuStatus, ramStatus, netStatus;
    private Button startStop;
    private Handler handler;
    private Switch cpuEnabled, ramEnabled, netEnabled;

    public MainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        handler = new Handler();

        cpuStatus = (TextView) rootView.findViewById(R.id.cpu_status);
        ramStatus = (TextView) rootView.findViewById(R.id.ram_status);
        netStatus = (TextView) rootView.findViewById(R.id.net_status);

        cpuEnabled = (Switch) rootView.findViewById(R.id.cpu_enabled);
        ramEnabled = (Switch) rootView.findViewById(R.id.ram_enabled);
        netEnabled = (Switch) rootView.findViewById(R.id.net_enabled);

        startStop = (Button) rootView.findViewById(R.id.start_stop);
        if (ThreadSpawn.isRunning())
            startStop.setText(R.string.stop);

        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ThreadSpawn.isStarted()) {
                    getActivity().startService(new Intent(getActivity().getApplicationContext(), ThreadSpawn.class));
                } else if (ThreadSpawn.isRunning()) {
                    ThreadSpawn.stopSpawner();
                } else {
                    ThreadSpawn.startSpawner();
                }
            }
        });

        cpuEnabled.setOnCheckedChangeListener(this);
        ramEnabled.setOnCheckedChangeListener(this);
        netEnabled.setOnCheckedChangeListener(this);

        cpuEnabled.setChecked(true);
        ramEnabled.setChecked(true);
        netEnabled.setChecked(true);

        // the following was shamelessly stolen from: https://developer.android.com/guide/topics/ui/controls/spinner.html
        Spinner spinner = (Spinner) rootView.findViewById(R.id.mode_selector);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.mode_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(this);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.INSTANCE.bus().register(this);
    }

    @Override
    public void onPause() {
        BusProvider.INSTANCE.bus().unregister(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (!ThreadSpawn.isRunning())
            ThreadSpawn.killSpawner();

        super.onDestroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0: // low
                ThreadSpawn.switchModes(Constants.LOW);
                break;
            case 1: // medium
                ThreadSpawn.switchModes(Constants.MEDIUM);
                break;
            case 2: // high
                ThreadSpawn.switchModes(Constants.HIGH);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }

    @Subscribe
    public void runningStatus(final MessageTypes.RunningStatus status) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (ThreadSpawn.isRunning())
                    startStop.setText(R.string.stop);
                else
                    startStop.setText(R.string.start);
            }
        });
    }

    @Subscribe
    public void updateCpuStatus(final MessageTypes.CpuStatus status) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                cpuStatus.setText(status.getStatus());
            }
        });
    }

    @Subscribe
    public void updateRamStatus(final MessageTypes.RamStatus status) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                ramStatus.setText(status.getStatus());
            }
        });
    }

    @Subscribe
    public void updateNetStatus(final MessageTypes.NetStatus status) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                netStatus.setText(status.getStatus());
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == cpuEnabled.getId()) {
            if (isChecked)
                Constants.MODE.CPU_ENABLED = true;
            else
                Constants.MODE.CPU_ENABLED = false;
        } else if (buttonView.getId() == ramEnabled.getId()) {
            if (isChecked)
                Constants.MODE.RAM_ENABLED = true;
            else
                Constants.MODE.RAM_ENABLED = false;
        } else if (buttonView.getId() == netEnabled.getId()) {
            if (isChecked)
                Constants.MODE.NET_ENABLED = true;
            else
                Constants.MODE.NET_ENABLED = false;
        }
    }
}
