package dev.kakrizky.lightwind.response;

public class PageMeta {

    private int currentPage;
    private int itemsPerPage;
    private long totalData;
    private int totalPages;

    public PageMeta() {}

    public PageMeta(int currentPage, int itemsPerPage, long totalData, int totalPages) {
        this.currentPage = currentPage;
        this.itemsPerPage = itemsPerPage;
        this.totalData = totalData;
        this.totalPages = totalPages;
    }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getItemsPerPage() { return itemsPerPage; }
    public void setItemsPerPage(int itemsPerPage) { this.itemsPerPage = itemsPerPage; }

    public long getTotalData() { return totalData; }
    public void setTotalData(long totalData) { this.totalData = totalData; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
