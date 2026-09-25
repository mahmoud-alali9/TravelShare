package com.example.travelshare.ui.home;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.travelshare.R;
import com.example.travelshare.model.Photo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FiltersDialog extends Dialog {

    public static final int REQUEST_CODE_PICK_MAP = 2001;

    public interface OnFiltersAppliedListener {
        void onFiltersApplied(FilterCriteria criteria);
    }

    private OnFiltersAppliedListener listener;
    private Fragment parentFragment;

    private EditText     etStartDate, etEndDate;
    private Spinner      spinnerAuthor;
    private EditText     etLatitude, etLongitude, etRadius, etSimilarId;
    private TextView     tvSelectedCoords;
    private Button       btnReset, btnApply, btnPickOnMap;
    private ImageButton  btnClose;
    private List<Photo> allPhotos;

    private String startDate = "";
    private String endDate   = "";

    public FiltersDialog(@NonNull Context context, Fragment parentFragment, List<Photo> allPhotos, OnFiltersAppliedListener listener) {
        super(context);
        this.allPhotos = allPhotos;
        this.listener = listener;
        this.parentFragment = parentFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_filters);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = (int) (getContext().getResources()
                    .getDisplayMetrics().widthPixels * 0.95f);
            getWindow().setAttributes(params);
        }

        bindViews();
        setupSpinnerAuthor();
        setupDatePickers();
        setupButtons();
    }

    private void bindViews() {
        etStartDate      = findViewById(R.id.et_start_date);
        etEndDate        = findViewById(R.id.et_end_date);
        spinnerAuthor    = findViewById(R.id.spinner_author);
        etLatitude       = findViewById(R.id.et_latitude);
        etLongitude      = findViewById(R.id.et_longitude);
        etRadius         = findViewById(R.id.et_radius);
        etSimilarId      = findViewById(R.id.et_similar_id);
        tvSelectedCoords = findViewById(R.id.tv_selected_coords);
        btnReset         = findViewById(R.id.btn_reset);
        btnApply         = findViewById(R.id.btn_apply_filters);
        btnClose         = findViewById(R.id.btn_close_filters);
        btnPickOnMap     = findViewById(R.id.btn_pick_on_map);
    }

    private void setupSpinnerAuthor() {
        List<String> authors = new ArrayList<>();
        authors.add("Tous les auteurs");
        if (allPhotos != null) {
            for (Photo p : allPhotos) {
                if (p.getAuthorName() != null && !authors.contains(p.getAuthorName())) {
                    authors.add(p.getAuthorName());
                }
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                authors);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAuthor.setAdapter(adapter);
    }

    private void setupDatePickers() {
        if (etStartDate != null) etStartDate.setOnClickListener(v -> showDatePicker(true));
        if (etEndDate != null) etEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, year, month, day) -> {
            String date = String.format("%04d-%02d-%02d", year, month + 1, day);
            String displayDate = String.format("%02d/%02d/%04d", day, month + 1, year);
            if (isStart) {
                startDate = date;
                etStartDate.setText(displayDate);
            } else {
                endDate = date;
                etEndDate.setText(displayDate);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void setupButtons() {
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        if (btnPickOnMap != null) {
            btnPickOnMap.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), MapPickerActivity.class);
                parentFragment.startActivityForResult(intent, REQUEST_CODE_PICK_MAP);
            });
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                startDate = "";
                endDate   = "";
                if (etStartDate != null) etStartDate.setText("");
                if (etEndDate != null) etEndDate.setText("");
                if (spinnerAuthor != null) spinnerAuthor.setSelection(0);
                if (etLatitude != null) etLatitude.setText("");
                if (etLongitude != null) etLongitude.setText("");
                if (etRadius != null) etRadius.setText("");
                if (etSimilarId != null) etSimilarId.setText("");
                if (tvSelectedCoords != null) tvSelectedCoords.setText("Aucun lieu sélectionné");
                if (listener != null) listener.onFiltersApplied(new FilterCriteria());
                dismiss();
            });
        }

        if (btnApply != null) btnApply.setOnClickListener(v -> applyFilters());
    }

    public void updateLocation(double lat, double lng) {
        if (etLatitude != null) etLatitude.setText(String.valueOf(lat));
        if (etLongitude != null) etLongitude.setText(String.valueOf(lng));
        if (tvSelectedCoords != null) {
            tvSelectedCoords.setText(String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f", lat, lng));
        }
    }

    private void applyFilters() {
        FilterCriteria criteria = new FilterCriteria();

        if (spinnerAuthor != null) {
            criteria.setAuthor(spinnerAuthor.getSelectedItem().toString());
        }

        criteria.setStartDate(startDate);
        criteria.setEndDate(endDate);

        String latStr = etLatitude != null ? etLatitude.getText().toString().trim() : "";
        String lngStr = etLongitude != null ? etLongitude.getText().toString().trim() : "";
        String radStr = etRadius != null ? etRadius.getText().toString().trim() : "";

        if (!latStr.isEmpty() && !lngStr.isEmpty()) {
            if (radStr.isEmpty()) {
                Toast.makeText(getContext(), "Veuillez spécifier un rayon pour la recherche par lieu", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                criteria.setLatitude(Double.parseDouble(latStr));
                criteria.setLongitude(Double.parseDouble(lngStr));
                criteria.setRadius(Double.parseDouble(radStr));
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Coordonnées ou rayon invalides", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (etSimilarId != null && !etSimilarId.getText().toString().trim().isEmpty()) {
            criteria.setSimilarPhotoId(etSimilarId.getText().toString().trim());
        }

        if (listener != null) {
            listener.onFiltersApplied(criteria);
        }
        dismiss();
    }
}
