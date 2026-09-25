package com.example.travelshare.ui.search;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelshare.R;
import com.example.travelshare.ui.home.PhotoAdapter;

public class SearchFragment extends Fragment {

    private RecyclerView recycler;
    private PhotoAdapter adapter;
    private TextView     tvCount;
    private View         layoutEmptyState;
    private SearchViewModel viewModel;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialisation du ViewModel (Jetpack)
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        recycler         = view.findViewById(R.id.recycler_search);
        tvCount          = view.findViewById(R.id.tv_search_count);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        // Configuration de l'adaptateur
        adapter = new PhotoAdapter(requireContext(), PhotoAdapter.VIEW_LIST);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        // Observation des données (MVVM)
        setupObservers();

        // Gestion de la barre de recherche
        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchHandler.removeCallbacks(searchRunnable);
                viewModel.performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> viewModel.performSearch(newText);
                searchHandler.postDelayed(searchRunnable, 400);
                return true;
            }
        });
    }

    private void setupObservers() {
        // Observe les résultats de recherche
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), photos -> {
            adapter.submitList(photos);
            
            String query = viewModel.getQueryText().getValue();
            updateResultCount(photos.size(), query);
            
            // Gestion de l'état vide
            if (layoutEmptyState != null) {
                // On affiche l'état vide seulement si une recherche est en cours et n'a rien donné
                boolean noResults = (query != null && !query.isEmpty() && photos.isEmpty());
                layoutEmptyState.setVisibility(noResults ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void updateResultCount(int count, String query) {
        if (query == null || query.isEmpty()) {
            tvCount.setText("Tapez un mot-clé pour commencer");
        } else {
            tvCount.setText(count + " résultat" + (count > 1 ? "s" : "") + " pour \"" + query + "\"");
        }
    }
}
