package dev.pgrazor;

/**
 * PGBR's independently implemented raid-feed filter rules, matching the
 * functionality publicly documented for PGSharp's Raid Feed where our
 * data provider actually supplies the corresponding fields.
 *
 * This is NOT PGSharp source or an interface to a PGSharp-owned backend.
 * Unreported attributes cannot be interpreted as an affirmative match.
 */
final class RaidFeedFilters {
    final int bossDex;       // 0: any; 1..1025: confirmed boss
    final int minMinutes;    // 0..180; positive means remaining time required
    final int raidType;      // 0: any; nonzero is provider raw raid_level
    final int team;          // 0: any; 1..3 as returned by service
    final boolean hatchedOnly;

    RaidFeedFilters(int bossDex,int minMinutes,int raidType,int team,boolean hatchedOnly) {
        if (bossDex<0||bossDex>1025 || minMinutes<0||minMinutes>180
                || raidType<0||raidType>20 || team<0||team>3)
            throw new IllegalArgumentException("Raid feed filter out of range");
        this.bossDex=bossDex;
        this.minMinutes=minMinutes;
        this.raidType=raidType;
        this.team=team;
        this.hatchedOnly=hatchedOnly;
    }

    static RaidFeedFilters defaults() { return new RaidFeedFilters(0,0,0,0,false); }

    /**
     * Raw provider values: raidLevel is not reinterpreted as number of stars.
     * Expired raids and unknown expiration are excluded from actionable results.
     */
    boolean matches(int reportedBossDex,double reportedRaidType,int reportedTeam,
                    long hatchAt,long expiresAt,boolean confirmedLiveBoss,long now) {
        if(expiresAt<=now || expiresAt<=0) return false;
        if(minMinutes>0 && expiresAt-now<(long)minMinutes*60_000L) return false;
        if(raidType>0 && (reportedRaidType<=0 || reportedRaidType!=raidType)) return false;
        if(team>0 && reportedTeam!=team) return false;
        if(hatchedOnly && (!confirmedLiveBoss || hatchAt>now)) return false;
        if(bossDex>0 && (!confirmedLiveBoss || hatchAt>now || reportedBossDex!=bossDex))
            return false;
        return true;
    }

    String summary() {
        StringBuilder b=new StringBuilder("Reides");
        if(bossDex>0) b.append(" · #").append(bossDex);
        if(raidType>0) b.append(" · Tipo ").append(raidType);
        if(minMinutes>0) b.append(" · +").append(minMinutes).append(" min");
        if(team>0) b.append(" · Equipe ").append(team);
        if(hatchedOnly) b.append(" · Chefe confirmado");
        return b.toString();
    }
}
