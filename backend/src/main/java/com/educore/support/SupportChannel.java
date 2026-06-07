package com.educore.support;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "support_channels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Teacher who owns this channel */
    private Long teacherId;

    /** Group name e.g. "دعم علمي", "دعم أكاديمي" */
    private String groupName;

    /** Grade filter — null means show for all grades */
    private String grade;

    /** Display label e.g. "واتساب المجموعة" */
    private String label;

    /** WHATSAPP | TELEGRAM | LINK */
    @Enumerated(EnumType.STRING)
    private ChannelType type;

    /** Phone number or URL */
    private String value;

    private Integer displayOrder;

    public enum ChannelType { WHATSAPP, TELEGRAM, LINK }
}
