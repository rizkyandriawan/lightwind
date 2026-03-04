package dev.kakrizky.lightwind.response;

public class BulkDeleteResult {

    private int deletedCount;

    public BulkDeleteResult() {}

    public BulkDeleteResult(int deletedCount) {
        this.deletedCount = deletedCount;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
    }
}
