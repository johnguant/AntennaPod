package de.danoeh.antennapod.core.feed;

import android.database.Cursor;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

import de.danoeh.antennapod.core.storage.PodDBAdapter;

@Entity(tableName = "SimpleChapters", indices = {
		@Index(name = "SimpleChapters_feeditem", value = "feeditem")
})
public abstract class Chapter extends FeedComponent {

	/** Defines starting point in milliseconds. */
	@ColumnInfo(name = "start")
			@Nullable
    Long start;
    @ColumnInfo(name = "title")
	String title;
    @ColumnInfo(name = "link")
	String link;

	@ColumnInfo(name = "feeditem")
	@Nullable
	Integer feedItemId;

	@ColumnInfo(name = "type")
	@Nullable
	Integer chapterType;

	@Ignore
	Chapter() {
	}
	
	Chapter(long start) {
		super();
		this.start = start;
	}

	Chapter(long start, String title, FeedItem item, String link) {
		super();
		this.start = start;
		this.title = title;
		this.link = link;
	}

	public static Chapter fromCursor(Cursor cursor, FeedItem item) {
		int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
		int indexTitle = cursor.getColumnIndex(PodDBAdapter.KEY_TITLE);
		int indexStart = cursor.getColumnIndex(PodDBAdapter.KEY_START);
		int indexLink = cursor.getColumnIndex(PodDBAdapter.KEY_LINK);
		int indexChapterType = cursor.getColumnIndex(PodDBAdapter.KEY_CHAPTER_TYPE);

		long id = cursor.getLong(indexId);
		String title = cursor.getString(indexTitle);
		long start = cursor.getLong(indexStart);
		String link = cursor.getString(indexLink);
		int chapterType = cursor.getInt(indexChapterType);

		Chapter chapter = null;
		switch (chapterType) {
			case SimpleChapter.CHAPTERTYPE_SIMPLECHAPTER:
				chapter = new SimpleChapter(start, title, item, link);
				break;
			case ID3Chapter.CHAPTERTYPE_ID3CHAPTER:
				chapter = new ID3Chapter(start, title, item, link);
				break;
			case VorbisCommentChapter.CHAPTERTYPE_VORBISCOMMENT_CHAPTER:
				chapter = new VorbisCommentChapter(start, title, item, link);
				break;
		}
		chapter.setId(id);
		return chapter;
	}


	public abstract int getChapterType();

	public long getStart() {
		return start;
	}

	public String getTitle() {
		return title;
	}

	public String getLink() {
		return link;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setLink(String link) {
		this.link = link;
	}

    @Override
    public String getHumanReadableIdentifier() {
        return title;
    }
}
