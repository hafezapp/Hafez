package org.hrana.hafez.adapter;

import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import org.hrana.hafez.R;
import org.hrana.hafez.model.LegalContact;
import org.hrana.hafez.model.LegalContact.LAW_TYPE;
import org.hrana.hafez.presenter.contract.IViewContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * Simple Adapter for Legal Contacts.
 * Takes {@link IViewContract.LegalContactsView} as parameter to respond to view interactions.
 */
public class LegalContactsAdapter extends RecyclerView.Adapter<LegalContactsAdapter.ViewHolder> {

    private static final String TAG = "LegalContactsAdapter";
    private List<LegalContact> mDataset;
    private List<LegalContact> mFilteredDataset;
    private final IViewContract.LegalContactsView fragment;
    private List<LegalContact> selectedContacts;

    public LegalContactsAdapter(IViewContract.LegalContactsView view) {
        this.mDataset = new ArrayList<>();
        this.mFilteredDataset = new ArrayList<>();
        this.fragment = view;
        this.selectedContacts = new ArrayList<>();
    }

    public void addItems(List<LegalContact> items) {
        mDataset.addAll(items);
        mFilteredDataset.addAll(items);
    }

    @Override
    public LegalContactsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_legalcontact, parent, false);

        final LegalContactsAdapter.ViewHolder holder = new ViewHolder(view, fragment);
        holder.contactLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LegalContact c = mFilteredDataset.get(holder.getAdapterPosition());
                if (!selectedContacts.contains(c)) {
                    selectedContacts.add(c);
                    holder.expandedView.setVisibility(View.VISIBLE);
                } else {
                    selectedContacts.remove(c);
                    holder.expandedView.setVisibility(View.GONE);
                }
            }
        });

        holder.officePhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("tel:" + holder.officePhone.getText().toString()));
                fragment.launchExternalIntent(intent);
            }
        });

        holder.mobilePhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("tel:" + holder.mobilePhone.getText().toString()));
                fragment.launchExternalIntent(intent);
            }
        });

        holder.address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("geo:" + holder.address.getText().toString()));
                fragment.launchExternalIntent(intent);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(final LegalContactsAdapter.ViewHolder holder, final int position) {
        if (!selectedContacts.contains(mFilteredDataset.get(position))) {
            holder.expandedView.setVisibility(View.GONE);
        } else {
            holder.expandedView.setVisibility(View.VISIBLE);
        }

        for (View v : holder.tagArray) {
            v.setVisibility(View.GONE);
        }

        /*
         * @Todo @Improvement
         * If receive image files for all the contacts with filenames we can recognize,
         * add pictures instead of the placeholder image by using
         * holder.contactImage.setImageResource(...);
         */
        holder.officePhone.setVisibility(View.VISIBLE);

        holder.mName.setText(mFilteredDataset.get(position).getName());
        holder.address.setText(mFilteredDataset.get(position).getAddress());
        holder.address.setAutoLinkMask(Linkify.MAP_ADDRESSES);
        if (mFilteredDataset.get(position).getPhone() != null) {
            holder.officePhone.setText(mFilteredDataset.get(position).getPhone());
        } else {
            holder.officePhone.setVisibility(View.GONE);
        }
        holder.mobilePhone.setText(mFilteredDataset.get(position).getMobile());

        for (LAW_TYPE t : mFilteredDataset.get(position).getLawType()) {
            if (t != null) {
                showLawType(holder, t);
            }
        }
    }


    private void showLawType(LegalContactsAdapter.ViewHolder holder, LAW_TYPE type) {
        int which = type.getIndex();
        if (holder != null
                && which > 0
                && which <= holder.tagArray.length) {
            holder.tagArray[which - 1].setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        if (mFilteredDataset != null) {
            return mFilteredDataset.size();
        }
        return 0;
    }

    public void filterContacts(Set<LAW_TYPE> filter) {
        mFilteredDataset.clear();
        for (LAW_TYPE item : filter) {
            for (LegalContact contact : mDataset) {
                if (contact.getLawType().contains(item)
                        && !mFilteredDataset.contains(contact)) {
                    mFilteredDataset.add(contact);
                }
            }
        }
        if (mFilteredDataset.isEmpty()) {
            Log.e(TAG, "Empty filter results (" + filter.size() + " filters applied)");
        }

        notifyDataSetChanged();
    }

    public void clearFilters() {
        mFilteredDataset.clear();
        mFilteredDataset.addAll(mDataset);
        notifyDataSetChanged();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.name)
        TextView mName;
        @BindView(R.id.address)
        TextView address;
        @BindView(R.id.phone_mobile)
        TextView mobilePhone;
        @BindView(R.id.phone_office)
        TextView officePhone;
        @BindView(R.id.expanded_contact_view)
        RelativeLayout expandedView;
        @BindView(R.id.linearlayout_contact)
        LinearLayout contactLayout;
        @BindView(R.id.contact_picture)
        RoundedImageView contactImage;

        @BindView(R.id.tag_1)
        TextView tag1;
        @BindView(R.id.tag_2)
        TextView tag2;
        @BindView(R.id.tag_3)
        TextView tag3;
        @BindView(R.id.tag_4)
        TextView tag4;
        @BindView(R.id.tag_5)
        TextView tag5;
        @BindView(R.id.tag_6)
        TextView tag6;

        private IViewContract.LegalContactsView view;
        private View[] tagArray;

        public ViewHolder(View itemView, IViewContract.LegalContactsView view) {
            super(itemView);
            this.view = view;
            ButterKnife.bind(this, itemView);
            itemView.setClickable(true);
            this.tagArray = new View[]{tag1, tag2, tag3, tag4, tag5, tag6};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                StateListAnimator animator = new StateListAnimator();
                animator.addState(new int[]{android.R.attr.enabled, android.R.attr.state_pressed,
                                android.R.attr.state_checked},
                        ObjectAnimator.ofFloat(contactLayout, "elevation", 8));
                contactLayout.setStateListAnimator(animator);
            }
        }

        @OnClick(R.id.linearlayout_contact)
        public void onClick() {
            View v = contactLayout;
            v.setSelected(!v.isSelected());
        }

    }

}

