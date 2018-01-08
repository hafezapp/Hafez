package org.hrana.hafez.view.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import org.hrana.hafez.R;
import org.hrana.hafez.adapter.LegalContactsAdapter;
import org.hrana.hafez.di.BaseApplication;
import org.hrana.hafez.di.component.DaggerILegalPresenterComponent;
import org.hrana.hafez.di.module.LegalPresenterModule;
import org.hrana.hafez.model.LegalContact;
import org.hrana.hafez.presenter.contract.IViewContract;
import org.hrana.hafez.presenter.LegalContactPresenter;
import org.hrana.hafez.view.button.FilterTouchButton;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Simple fragment to display legal contact information.
 */
public class LegalContactFragment extends Fragment
        implements IViewContract.LegalContactsView,
        View.OnClickListener {

    @Inject SharedPreferences preferences;
    @Inject LegalContactsAdapter adapter;
    @Inject LegalContactPresenter presenter;
    private Set<LegalContact.LAW_TYPE> filters;
    private CheckBox[] boxes;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;
    private Unbinder unbinder;
    @BindView(R.id.fragment_title)
    TextView title;
    @BindView(R.id.filter_view)
    FilterTouchButton filterView;

    public static LegalContactFragment newInstance() {
        return new LegalContactFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filters = new HashSet<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lawyers, container, false);
        unbinder = ButterKnife.bind(this, view);

        // DI
        DaggerILegalPresenterComponent.builder()
                .legalPresenterModule(new LegalPresenterModule(this))
                .iApplicationComponent(BaseApplication.get(getActivity())
                        .getComponent())
                .build()
                .inject(this);

        title.setText(getString(R.string.legal_directory));
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        adapter.addItems(presenter.parseContacts(getContext())); // from csv
        adapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof CheckBox) {
            onFilterClick(v);
        }
    }

    @Override
    public void launchExternalIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void onFilterClick(View view) {
        LegalContact.LAW_TYPE type = null;
        CheckBox targetFilter = (CheckBox) view;
        switch (targetFilter.getId()) {
            case (R.id.filter1):
                type = LegalContact.LAW_TYPE.ONE;
                break;
            case (R.id.filter2):
                type = LegalContact.LAW_TYPE.TWO;
                break;
            case (R.id.filter3):
                type = LegalContact.LAW_TYPE.THREE;
                break;
            case (R.id.filter4):
                type = LegalContact.LAW_TYPE.FOUR;
                break;
            case (R.id.filter5):
                type = LegalContact.LAW_TYPE.FIVE;
                break;
            case (R.id.filter6):
                type = LegalContact.LAW_TYPE.SIX;
                break;
            default:
                break;
        }
        if (type != null) {
            if (targetFilter.isChecked()) {
                filters.add(type);
            } else {
                filters.remove(type);
            }
        }
    }


    @OnClick(R.id.filter_view)
    public void showFilterOptions() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_advanced_search, null);

        boxes = new CheckBox[]{dialogView.findViewById(R.id.filter1),
                dialogView.findViewById(R.id.filter2),
                dialogView.findViewById(R.id.filter3),
                dialogView.findViewById(R.id.filter4),
                dialogView.findViewById(R.id.filter5),
                dialogView.findViewById(R.id.filter6)};

        for (CheckBox target : boxes) {
            target.setOnClickListener(this);
        }

        if (!filters.isEmpty()) {
            for (LegalContact.LAW_TYPE type : filters) {
                boxes[type.getIndex() - 1].setChecked(true);
            }
        }

        builder.setView(dialogView);
        builder
                .setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!filters.isEmpty()) {
                            adapter.filterContacts(filters);
                        } else {
                            Log.e("LegalContent", "Filter size " + filters.size());
                            Toast.makeText(getContext(), "No results found for this search",
                                    Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.clear), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        filters.clear();
                        adapter.clearFilters();
                        dialog.dismiss();
                    }
                })
                .create()
                .show();

    }
}
