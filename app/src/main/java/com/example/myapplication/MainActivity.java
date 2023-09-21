package com.example.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ListView listView;
    private ProductListAdapter adapter;
    private ArrayList<Product> productList;
    private ArrayList<String> productTypes;
    private boolean showingProductTypes = true;
    private String selectedCategory;
    private boolean toastShown = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myappphone);
        listView = findViewById(R.id.listView);
        productList = new ArrayList<>();
        adapter = new ProductListAdapter(this, productList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Product selectedProduct = productList.get(position);
            showProductDetailsDialog(selectedProduct);
        });
        //
        //
        //
        EditText brandEditText = findViewById(R.id.brandEditText);
//        Button searchButton = findViewById(R.id.searchButton);
        brandEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String brandQuery = brandEditText.getText().toString().trim();
                if (!brandQuery.isEmpty()) {
                    loadProductsForBrand(brandQuery,selectedCategory);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String brandQuery = brandEditText.getText().toString().trim();
                if (brandQuery.isEmpty()) {
                    loadProductsForCategory(selectedCategory);
                }
            }
        });

        //
        //
        //
        productTypes = new ArrayList<>();
        productTypes.add("Blush");
        productTypes.add("Bronzer");
        productTypes.add("Eyebrow");
        productTypes.add("Eyeliner");
        productTypes.add("Eyeshadow");
        productTypes.add("Foundation");
        productTypes.add("Lip liner");
        productTypes.add("Lipstick");
        productTypes.add("Mascara");
        productTypes.add("Nail polish");
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, productTypes);
        ListView categoryListView = findViewById(R.id.categoryListView);
        categoryListView.setAdapter(categoryAdapter);

        categoryListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = productTypes.get(position);
            loadProductsForCategory(selectedCategory);
            brandEditText.setVisibility(View.VISIBLE);
            if(!brandEditText.getText().toString().trim().isEmpty()){
                brandEditText.setText("");
            }
//            searchButton.setVisibility(View.VISIBLE);
        });
    }
    private void showProductDetailsDialog(Product product) {
        String imageView = product.getImageUrl();
        String productName = product.getName();
        String productBrand = product.getBrand();
        String productCategory = product.getCategory();
        String productPrice = product.getPrice();
        List<String> productColors = product.getProductColors();
        ProductDialog dialog = new ProductDialog(imageView,productName, productBrand, productCategory, productPrice,productColors);
        dialog.show(getSupportFragmentManager(), "product_dialog");
    }

    private void loadProductsForCategory(String productType) {
        productList.clear();
        adapter.notifyDataSetChanged();
        new FetchProductDataTask().execute(productType);
        showingProductTypes = false;
    }
    private void loadProductsForBrand(String brand,String category) {
        productList.clear();
        adapter.notifyDataSetChanged();
        new FetchProductDataTaskBrand().execute(brand, category);
        showingProductTypes = false;
    }
    private class FetchProductDataTaskBrand extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if (params.length < 2) {
                return null;
            }
            String brand = params[0];
            String category = params[1];
            String apiUrl = "http://makeup-api.herokuapp.com/api/v1/products.json?brand=" + brand + "&product_type=" + category;
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
                    return s.hasNext() ? s.next() : "";
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                parseProductData(result);
            }
            if (result != null && result.equals("[]") && !toastShown) {
                Toast.makeText(MainActivity.this, "Ничего не найдено :(", Toast.LENGTH_SHORT).show();
                toastShown = true;
            }
        }
        private void parseProductData(String json) {
            try {
                productList.clear();
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String name = jsonObject.optString("name");
                    String brand = jsonObject.optString("brand");
                    String imageUrl = jsonObject.optString("api_featured_image");
                    String price = jsonObject.optString("price");
                    String price_sign = jsonObject.optString("price_sign");
                    String category = jsonObject.optString("category");
                    JSONArray colorsArray = jsonObject.optJSONArray("product_colors");
                    List<String> productColors = new ArrayList<>();
                    if (colorsArray != null) {
                        for (int j = 0; j < colorsArray.length(); j++) {
                            JSONObject colorObject = colorsArray.getJSONObject(j);
                            String hexColor = colorObject.optString("hex_value");
                            productColors.add(hexColor);
                        }
                    }
                    Product product = new Product(name, brand, "https:"+imageUrl,price+price_sign,price_sign,category);
                    product.setProductColors(productColors);
                    productList.add(product);


                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class FetchProductDataTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }
            String productType = params[0];
            String apiUrl = "http://makeup-api.herokuapp.com/api/v1/products.json?product_type=" + productType;
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
                    return s.hasNext() ? s.next() : "";
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                parseProductData(result);
            }
        }

        private void parseProductData(String json) {
            try {
                productList.clear();
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String name = jsonObject.optString("name");
                    String brand = jsonObject.optString("brand");
                    String imageUrl = jsonObject.optString("api_featured_image");
                    String price = jsonObject.optString("price");
                    String price_sign = jsonObject.optString("price_sign");
                    String category = jsonObject.optString("category");
                    JSONArray colorsArray = jsonObject.optJSONArray("product_colors");
                    List<String> productColors = new ArrayList<>();
                    if (colorsArray != null) {
                        for (int j = 0; j < colorsArray.length(); j++) {
                            JSONObject colorObject = colorsArray.getJSONObject(j);
                            String hexColor = colorObject.optString("hex_value");
                            productColors.add(hexColor);
                        }
                    }
                    Product product = new Product(name, brand, "https:"+imageUrl,price+price_sign,price_sign,category);
                    product.setProductColors(productColors);
                    productList.add(product);


                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    private class ProductListAdapter extends ArrayAdapter<Product> {
        public ProductListAdapter(@NonNull AppCompatActivity context, ArrayList<Product> productList) {
            super(context, 0, productList);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }

            ImageView productImageView = convertView.findViewById(R.id.productImageView);
            TextView nameTextView = convertView.findViewById(R.id.nameTextView);
            Product product = getItem(position);

            if (product != null) {
                nameTextView.setText(product.getName());
                if (product.getImageUrl() != null) {
                    Picasso.get().load(product.getImageUrl()).into(productImageView);
                } else {
                    productImageView.setImageResource(android.R.color.black);
                }
            }

            return convertView;
        }
    }
    public class Product {
        private String name;
        private String brand;
        private String imageUrl;
        private String price;
        private String price_sign;
        private  String category;
        private List<String> colors;


        public Product(String name, String brand, String imageUrl, String price, String price_sign,String category) {
            this.name = name;
            this.brand = brand;
            this.imageUrl = imageUrl;
            this.price=price;
            this.price_sign = price_sign;
            this.category = category;
            this.colors = new ArrayList<>();

        }
        public List<String> getProductColors() {
            return colors;
        }

        public void setProductColors(List<String> productColors) {
            this.colors = productColors;
        }
        public String getBrand() {
            return brand;
        }

        public String getName() {
            return name;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getPriceSign() {
            return price_sign;
        }

        public void setPriceSign(String price_sign) {
            this.price_sign = price_sign;
        }


        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }
    public static class ProductDialog extends DialogFragment {
        private String productName;
        private String productBrand;
        private String productCategory;
        private String productPrice;
        private String imagedetailUrl;
        private List<String> productColors;

        public ProductDialog(String imagedetailUrl,String productName, String productBrand, String productCategory, String productPrice,List<String> productColors) {
            this.imagedetailUrl = imagedetailUrl;
            this.productName = productName;
            this.productBrand = productBrand;
            this.productCategory = productCategory;
            this.productPrice = productPrice;
            this.productColors = productColors;

        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.product_dialog, null);

            ImageView imagedetailUrlView = dialogView.findViewById(R.id.detailImageView);
            TextView nameTextView = dialogView.findViewById(R.id.productNameTextView);
            TextView brandTextView = dialogView.findViewById(R.id.productBrandTextView);
            TextView categoryTextView = dialogView.findViewById(R.id.productCategoryTextView);
            TextView priceTextView = dialogView.findViewById(R.id.productPriceTextView);
            GridView colorPaletteGridView = dialogView.findViewById(R.id.colorPaletteGridView);
            Button closeButton = dialogView.findViewById(R.id.closeButton);

            Picasso.get().load(imagedetailUrl).into(imagedetailUrlView);
            nameTextView.setText(productName);
            brandTextView.setText("Бренд: " + productBrand);
            categoryTextView.setText("Категория: " + productCategory);
            priceTextView.setText("Цена: " + productPrice);
            ColorPaletteAdapter colorAdapter = new ColorPaletteAdapter(requireContext(), productColors);
            colorPaletteGridView.setAdapter(colorAdapter);


            builder.setView(dialogView);

            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });
            return builder.create();
        }
    }
    public static class ColorPaletteAdapter extends BaseAdapter {
        private Context context;
        private List<String> colors;

        public ColorPaletteAdapter(Context context, List<String> colors) {
            this.context = context;
            this.colors = colors;
        }

        @Override
        public int getCount() {
            return colors.size();
        }

        @Override
        public Object getItem(int position) {
            return colors.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.color_item, parent, false);
            }

            ImageView colorImageView = view.findViewById(R.id.colorImageView);

            String hexColor = colors.get(position);

            try {
                int color = Color.parseColor(hexColor);
                colorImageView.setBackgroundColor(color);
            } catch (IllegalArgumentException e) {
                colorImageView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }

            return view;
        }
    }
}
