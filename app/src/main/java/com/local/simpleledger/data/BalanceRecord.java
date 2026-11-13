package com.local.simpleledger.data;

/** One change to the savings balance: a weekly settlement, deposit, withdrawal or adjustment. */
public final class BalanceRecord {

    public static final String TYPE_SETTLE = "settle";
    public static final String TYPE_DEPOSIT = "deposit";
    public static final String TYPE_WITHDRAW = "withdraw";
    public static final String TYPE_ADJUST = "adjust";

    public static final String[] COLUMNS = {
            "_id", "type", "title", "amount_cents", "occurred_at",
            "week_start", "week_end", "budget_cents", "spent_cents", "created_at"
    };

    public final long id;
    public final String type;
    public final String title;
    public final long amountCents;
    public final String occurredAt;
    public final String weekStart;
    public final String weekEnd;
    public final long budgetCents;
    public final long spentCents;
    public final long createdAt;

    public BalanceRecord(long id, String type, String title, long amountCents,
                         String occurredAt, String weekStart, String weekEnd,
                         long budgetCents, long spentCents, long createdAt) {
        this.id = id;
        this.type = type == null ? "" : type;
        this.title = title == null ? "" : title;
        this.amountCents = amountCents;
        this.occurredAt = occurredAt == null ? "" : occurredAt;
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        this.budgetCents = budgetCents;
        this.spentCents = spentCents;
        this.createdAt = createdAt;
    }

    public boolean isSettlement() {
        return TYPE_SETTLE.equals(type);
    }

    public boolean isDeposit() {
        return TYPE_DEPOSIT.equals(type);
    }

    public boolean isWithdraw() {
        return TYPE_WITHDRAW.equals(type);
    }

    public boolean isAdjustment() {
        return TYPE_ADJUST.equals(type);
    }
}
